package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.data.wallet.EthereumKeyEngine
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumReceiveAddress
import network.bahn.androidcryptowallet.domain.model.InvalidEthereumMnemonicException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EthereumWalletRepositoryImplTest {
    @Test
    fun createWritesWalletForChosenNetworkOnly() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val networkStore = FakeSelectedEthereumNetworkStore()
        val repo = createRepository(engine = engine, store = store, networkStore = networkStore)

        repo.createWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = null)

        val wallets = repo.observeWallets().first()
        assertEquals(1, wallets.size)
        assertEquals(EthereumNetwork.SEPOLIA, wallets.single().network)
        assertEquals(SEPOLIA_ADDRESS, wallets.single().address)
        assertEquals(VALID_WORDS.joinToString(" "), store.saved[wallets.single().id]?.mnemonic)
        assertEquals(null, store.saved[wallets.single().id]?.passphrase)
        assertEquals(EthereumNetwork.SEPOLIA, store.saved[wallets.single().id]?.network)
        assertEquals(1, engine.validateCalls)
        assertEquals(1, engine.deriveCalls)

        networkStore.setNetwork(EthereumNetwork.MAINNET)
        assertTrue(repo.observeWallets().first().isEmpty())
    }

    @Test
    fun createRejectsInvalidMnemonic() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(engine = engine, store = store)

        try {
            repo.createWallet(EthereumNetwork.SEPOLIA, listOf("not", "valid"), null)
            error("expected InvalidEthereumMnemonicException")
        } catch (_: InvalidEthereumMnemonicException) {
        }

        assertTrue(repo.observeWallets().first().isEmpty())
        assertTrue(store.saved.isEmpty())
        assertEquals(0, engine.deriveCalls)
    }

    @Test
    fun createPersistsPassphrase() = runTest {
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(store = store)

        repo.createWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = "secret")

        val id = repo.observeWallets().first().single().id
        assertEquals("secret", store.saved[id]?.passphrase)
    }

    private fun createRepository(
        engine: FakeEthereumKeyEngine = FakeEthereumKeyEngine(),
        store: FakeEthereumMnemonicStore = FakeEthereumMnemonicStore(),
        walletDao: FakeEthereumWalletDao = FakeEthereumWalletDao(),
        networkStore: FakeSelectedEthereumNetworkStore = FakeSelectedEthereumNetworkStore(),
    ) = EthereumWalletRepositoryImpl(
        keyEngine = engine,
        mnemonicStore = store,
        walletDao = walletDao,
        selectedEthereumNetworkStore = networkStore,
    )
}

private val VALID_WORDS = List(11) { "abandon" } + "about"
private const val SEPOLIA_ADDRESS = "0x1111111111111111111111111111111111111111"

private class FakeEthereumKeyEngine : EthereumKeyEngine {
    var validateCalls = 0
    var deriveCalls = 0

    override fun generateMnemonic(): List<String> = VALID_WORDS

    override fun validateMnemonic(words: List<String>) {
        validateCalls += 1
        if (words != VALID_WORDS) throw InvalidEthereumMnemonicException("invalid")
    }

    override fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
    ): EthereumReceiveAddress {
        deriveCalls += 1
        return EthereumReceiveAddress(address = SEPOLIA_ADDRESS, index = 0)
    }
}

private class FakeEthereumMnemonicStore : EthereumMnemonicStore {
    data class Saved(
        val mnemonic: String,
        val passphrase: String?,
        val network: EthereumNetwork,
    )

    val saved = mutableMapOf<String, Saved>()

    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: EthereumNetwork,
    ) {
        saved[walletId] = Saved(mnemonic, passphrase, network)
    }

    override fun listHdWalletIds(): List<String> = saved.keys.sorted()

    override fun loadNetwork(walletId: String): EthereumNetwork? = saved[walletId]?.network

    override fun loadMnemonic(walletId: String): String? = saved[walletId]?.mnemonic

    override fun loadPassphrase(walletId: String): String? = saved[walletId]?.passphrase
}

private class FakeSelectedEthereumNetworkStore(
    initial: EthereumNetwork = EthereumNetwork.SEPOLIA,
) : SelectedEthereumNetworkStore {
    private val network = MutableStateFlow(initial)
    override val selectedNetwork: Flow<EthereumNetwork> = network
    override suspend fun setNetwork(network: EthereumNetwork) {
        this.network.value = network
    }
}

private class FakeEthereumWalletDao : EthereumWalletDao {
    private val items = MutableStateFlow<List<EthereumWalletEntity>>(emptyList())

    override fun observeByNetwork(network: String): Flow<List<EthereumWalletEntity>> =
        items.map { rows -> rows.filter { it.network == network } }

    override suspend fun findByNetworkAndAddress(
        network: String,
        address: String,
    ): EthereumWalletEntity? = items.value.find {
        it.network == network && it.address == address
    }

    override suspend fun insert(entity: EthereumWalletEntity) {
        items.update { it + entity }
    }

    override suspend fun insertIgnore(entity: EthereumWalletEntity) {
        items.update { rows ->
            if (rows.any { it.id == entity.id }) rows else rows + entity
        }
    }
}
