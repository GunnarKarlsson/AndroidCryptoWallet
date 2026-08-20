package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.BitcoinReceiveAddressDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinReceiveAddressEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.InvalidBitcoinMnemonicException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitcoinWalletRepositoryImplTest {
    @Test
    fun createWritesAddressesForBothNetworks() = runTest {
        val engine = FakeBitcoinKeyEngine()
        val store = FakeBitcoinMnemonicStore()
        val networkStore = FakeWalletSelectedBitcoinNetworkStore()
        val repo = createRepository(engine = engine, store = store, networkStore = networkStore)

        repo.createWallet(VALID_WORDS, passphrase = null)

        val testnet = repo.observeReceiveAddress().first()
        assertEquals(BitcoinNetwork.TESTNET4, testnet?.network)
        assertEquals(TESTNET_ADDRESS, testnet?.address)
        assertEquals(0, testnet?.index)
        assertEquals(BitcoinScriptType.BIP84, testnet?.scriptType)
        assertTrue(repo.observeWalletExists().first())
        assertEquals(VALID_WORDS.joinToString(" "), store.savedMnemonic)
        assertNull(store.savedPassphrase)
        assertEquals(1, engine.deriveCalls)
        assertEquals(1, engine.validateCalls)

        networkStore.setNetwork(BitcoinNetwork.MAINNET)
        val mainnet = repo.observeReceiveAddress().first()
        assertEquals(BitcoinNetwork.MAINNET, mainnet?.network)
        assertEquals(MAINNET_ADDRESS, mainnet?.address)
        assertEquals(1, engine.deriveCalls)
    }

    @Test
    fun importRejectsInvalidMnemonic() = runTest {
        val engine = FakeBitcoinKeyEngine()
        val store = FakeBitcoinMnemonicStore()
        val repo = createRepository(engine = engine, store = store)

        try {
            repo.importWallet("not a real mnemonic", null)
            error("expected InvalidBitcoinMnemonicException")
        } catch (_: InvalidBitcoinMnemonicException) {
        }

        assertFalse(repo.observeWalletExists().first())
        assertNull(store.savedMnemonic)
        assertEquals(0, engine.deriveCalls)
    }

    @Test
    fun observeFollowsNetworkSwitchWithoutTouchingSecrets() = runTest {
        val engine = FakeBitcoinKeyEngine()
        val store = FakeBitcoinMnemonicStore()
        val networkStore = FakeWalletSelectedBitcoinNetworkStore()
        val repo = createRepository(engine = engine, store = store, networkStore = networkStore)

        repo.createWallet(VALID_WORDS, "secret-pass")
        assertEquals(1, engine.deriveCalls)
        assertEquals("secret-pass", store.savedPassphrase)

        networkStore.setNetwork(BitcoinNetwork.MAINNET)
        assertEquals(MAINNET_ADDRESS, repo.observeReceiveAddress().first()?.address)

        networkStore.setNetwork(BitcoinNetwork.TESTNET4)
        assertEquals(TESTNET_ADDRESS, repo.observeReceiveAddress().first()?.address)

        assertEquals(1, engine.deriveCalls)
        assertEquals(1, store.saveCalls)
    }

    private fun createRepository(
        engine: FakeBitcoinKeyEngine = FakeBitcoinKeyEngine(),
        store: FakeBitcoinMnemonicStore = FakeBitcoinMnemonicStore(),
        networkStore: FakeWalletSelectedBitcoinNetworkStore = FakeWalletSelectedBitcoinNetworkStore(),
    ): BitcoinWalletRepositoryImpl = BitcoinWalletRepositoryImpl(
        keyEngine = engine,
        mnemonicStore = store,
        receiveAddressDao = FakeBitcoinReceiveAddressDao(),
        selectedBitcoinNetworkStore = networkStore,
    )
}

private val VALID_WORDS = List(12) { "abandon" }.dropLast(1) + "about"
private const val MAINNET_ADDRESS = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"
private const val TESTNET_ADDRESS = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34"

private class FakeBitcoinKeyEngine : BitcoinKeyEngine {
    var deriveCalls = 0
    var validateCalls = 0

    override fun generateMnemonic(): List<String> = VALID_WORDS

    override fun validateMnemonic(words: List<String>) {
        validateCalls++
        if (words != VALID_WORDS) {
            throw InvalidBitcoinMnemonicException("invalid")
        }
    }

    override fun deriveReceiveAddresses(
        mnemonicWords: List<String>,
        passphrase: String?,
    ): List<BitcoinReceiveAddress> {
        deriveCalls++
        return listOf(
            BitcoinReceiveAddress(BitcoinNetwork.TESTNET4, TESTNET_ADDRESS, 0),
            BitcoinReceiveAddress(BitcoinNetwork.MAINNET, MAINNET_ADDRESS, 0),
        )
    }
}

private class FakeBitcoinMnemonicStore : BitcoinMnemonicStore {
    var savedMnemonic: String? = null
    var savedPassphrase: String? = null
    var saveCalls = 0

    override fun hasWallet(): Boolean = savedMnemonic != null

    override fun save(mnemonic: String, passphrase: String?) {
        saveCalls++
        savedMnemonic = mnemonic
        savedPassphrase = passphrase
    }
}

private class FakeBitcoinReceiveAddressDao : BitcoinReceiveAddressDao {
    private val items = MutableStateFlow<Map<String, BitcoinReceiveAddressEntity>>(emptyMap())

    override fun observe(network: String): Flow<BitcoinReceiveAddressEntity?> =
        items.map { it[network] }

    override fun observeCount(): Flow<Int> = items.map { it.size }

    override suspend fun upsert(entity: BitcoinReceiveAddressEntity) {
        items.update { it + (entity.network to entity) }
    }

    override suspend fun upsertAll(entities: List<BitcoinReceiveAddressEntity>) {
        items.update { current -> current + entities.associateBy { it.network } }
    }
}

private class FakeWalletSelectedBitcoinNetworkStore : SelectedBitcoinNetworkStore {
    override val selectedNetwork = MutableStateFlow(BitcoinNetwork.TESTNET4)

    override suspend fun setNetwork(network: BitcoinNetwork) {
        selectedNetwork.value = network
    }
}
