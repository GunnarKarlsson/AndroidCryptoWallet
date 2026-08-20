package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.BitcoinAddressBalance
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.InvalidBitcoinMnemonicException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BitcoinWalletRepositoryImplTest {
    @Test
    fun createWritesWalletForChosenNetworkOnly() = runTest {
        val engine = FakeBitcoinKeyEngine()
        val store = FakeBitcoinMnemonicStore()
        val networkStore = FakeWalletSelectedBitcoinNetworkStore()
        val repo = createRepository(engine = engine, store = store, networkStore = networkStore)

        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, passphrase = null)

        val wallets = repo.observeWallets().first()
        assertEquals(1, wallets.size)
        assertEquals(BitcoinNetwork.TESTNET4, wallets.single().network)
        assertEquals(TESTNET_ADDRESS, wallets.single().receiveAddress)
        assertEquals(BitcoinScriptType.BIP84, wallets.single().scriptType)
        assertEquals(VALID_WORDS.joinToString(" "), store.saved[wallets.single().id]?.first)
        assertEquals(null, store.saved[wallets.single().id]?.second)
        assertEquals(listOf(BitcoinNetwork.TESTNET4), engine.deriveNetworks)
        assertEquals(1, engine.validateCalls)

        networkStore.setNetwork(BitcoinNetwork.MAINNET)
        assertTrue(repo.observeWallets().first().isEmpty())
    }

    @Test
    fun createRejectsInvalidMnemonic() = runTest {
        val engine = FakeBitcoinKeyEngine()
        val store = FakeBitcoinMnemonicStore()
        val repo = createRepository(engine = engine, store = store)

        try {
            repo.createWallet(BitcoinNetwork.TESTNET4, listOf("not", "valid"), null)
            error("expected InvalidBitcoinMnemonicException")
        } catch (_: InvalidBitcoinMnemonicException) {
        }

        assertTrue(repo.observeWallets().first().isEmpty())
        assertTrue(store.saved.isEmpty())
        assertTrue(engine.deriveNetworks.isEmpty())
    }

    @Test
    fun observeFollowsNetworkSwitchWithoutExtraDerive() = runTest {
        val engine = FakeBitcoinKeyEngine()
        val store = FakeBitcoinMnemonicStore()
        val networkStore = FakeWalletSelectedBitcoinNetworkStore()
        val repo = createRepository(engine = engine, store = store, networkStore = networkStore)

        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, "secret-pass")
        repo.createWallet(BitcoinNetwork.MAINNET, VALID_WORDS, null)
        assertEquals(listOf(BitcoinNetwork.TESTNET4, BitcoinNetwork.MAINNET), engine.deriveNetworks)

        assertEquals(TESTNET_ADDRESS, repo.observeWallets().first().single().receiveAddress)

        networkStore.setNetwork(BitcoinNetwork.MAINNET)
        assertEquals(MAINNET_ADDRESS, repo.observeWallets().first().single().receiveAddress)

        networkStore.setNetwork(BitcoinNetwork.TESTNET4)
        assertEquals(TESTNET_ADDRESS, repo.observeWallets().first().single().receiveAddress)

        assertEquals(2, engine.deriveNetworks.size)
        assertEquals(2, store.saved.size)
    }

    @Test
    fun refreshBalanceCachesSatoshisForWallet() = runTest {
        val remote = FakeWalletBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)

        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets().first().single().id
        repo.refreshBalance(id)

        val wallet = repo.observeWallet(id).first()
        assertEquals(12_345L, wallet?.confirmedBalanceSatoshis)
        assertEquals(100L, wallet?.unconfirmedBalanceSatoshis)
        assertEquals(1_700_000_000_000L, wallet?.balanceUpdatedAtMillis)
        assertEquals(listOf(TESTNET_ADDRESS), remote.addresses)
        assertEquals(listOf(BitcoinNetwork.TESTNET4), remote.networks)
    }

    private fun createRepository(
        engine: FakeBitcoinKeyEngine = FakeBitcoinKeyEngine(),
        store: FakeBitcoinMnemonicStore = FakeBitcoinMnemonicStore(),
        networkStore: FakeWalletSelectedBitcoinNetworkStore = FakeWalletSelectedBitcoinNetworkStore(),
        remote: FakeWalletBitcoinRemoteDataSource = FakeWalletBitcoinRemoteDataSource(),
    ): BitcoinWalletRepositoryImpl = BitcoinWalletRepositoryImpl(
        keyEngine = engine,
        mnemonicStore = store,
        walletDao = FakeBitcoinWalletDao(),
        selectedBitcoinNetworkStore = networkStore,
        remote = remote,
        timeProvider = TimeProvider { 1_700_000_000_000L },
    )
}

private val VALID_WORDS = List(12) { "abandon" }.dropLast(1) + "about"
private const val MAINNET_ADDRESS = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"
private const val TESTNET_ADDRESS = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34"

private class FakeBitcoinKeyEngine : BitcoinKeyEngine {
    val deriveNetworks = mutableListOf<BitcoinNetwork>()
    var validateCalls = 0

    override fun generateMnemonic(): List<String> = VALID_WORDS

    override fun validateMnemonic(words: List<String>) {
        validateCalls++
        if (words != VALID_WORDS) {
            throw InvalidBitcoinMnemonicException("invalid")
        }
    }

    override fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
        network: BitcoinNetwork,
    ): BitcoinReceiveAddress {
        deriveNetworks += network
        val address = when (network) {
            BitcoinNetwork.TESTNET4 -> TESTNET_ADDRESS
            BitcoinNetwork.MAINNET -> MAINNET_ADDRESS
        }
        return BitcoinReceiveAddress(network, address, 0)
    }
}

private class FakeBitcoinMnemonicStore : BitcoinMnemonicStore {
    val saved = mutableMapOf<String, Pair<String, String?>>()

    override fun save(walletId: String, mnemonic: String, passphrase: String?) {
        saved[walletId] = mnemonic to passphrase
    }
}

private class FakeBitcoinWalletDao : BitcoinWalletDao {
    private val items = MutableStateFlow<List<BitcoinWalletEntity>>(emptyList())

    override fun observeByNetwork(network: String): Flow<List<BitcoinWalletEntity>> =
        items.map { rows -> rows.filter { it.network == network } }

    override fun observeById(id: String): Flow<BitcoinWalletEntity?> =
        items.map { rows -> rows.find { it.id == id } }

    override suspend fun insert(entity: BitcoinWalletEntity) {
        items.update { it + entity }
    }

    override suspend fun updateBalance(
        id: String,
        confirmedSatoshis: Long,
        unconfirmedSatoshis: Long,
        updatedAtMillis: Long,
    ) {
        items.update { rows ->
            rows.map { row ->
                if (row.id != id) {
                    row
                } else {
                    row.copy(
                        confirmedBalanceSatoshis = confirmedSatoshis,
                        unconfirmedBalanceSatoshis = unconfirmedSatoshis,
                        balanceUpdatedAtMillis = updatedAtMillis,
                    )
                }
            }
        }
    }
}

private class FakeWalletBitcoinRemoteDataSource : BitcoinRemoteDataSource {
    val networks = mutableListOf<BitcoinNetwork>()
    val addresses = mutableListOf<String>()

    override suspend fun getBlockCount(network: BitcoinNetwork): Long = error("unused")

    override suspend fun getAddressBalance(
        network: BitcoinNetwork,
        address: String,
    ): BitcoinAddressBalance {
        networks += network
        addresses += address
        return BitcoinAddressBalance(confirmedSatoshis = 12_345L, unconfirmedSatoshis = 100L)
    }
}

private class FakeWalletSelectedBitcoinNetworkStore : SelectedBitcoinNetworkStore {
    override val selectedNetwork = MutableStateFlow(BitcoinNetwork.TESTNET4)

    override suspend fun setNetwork(network: BitcoinNetwork) {
        selectedNetwork.value = network
    }
}
