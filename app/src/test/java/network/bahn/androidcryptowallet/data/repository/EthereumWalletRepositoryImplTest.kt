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
import network.bahn.androidcryptowallet.data.remote.EthereumRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.EthereumKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.EthereumAddressBalance
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
        assertEquals(DEFAULT_ADDRESS, wallets.single().address)
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

    @Test
    fun restoreWritesWalletForChosenNetwork() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val networkStore = FakeSelectedEthereumNetworkStore()
        val repo = createRepository(engine = engine, store = store, networkStore = networkStore)

        repo.restoreWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = null)

        val wallets = repo.observeWallets().first()
        assertEquals(1, wallets.size)
        assertEquals(EthereumNetwork.SEPOLIA, wallets.single().network)
        assertEquals(DEFAULT_ADDRESS, wallets.single().address)
        assertEquals(VALID_WORDS.joinToString(" "), store.saved[wallets.single().id]?.mnemonic)
        assertEquals(null, store.saved[wallets.single().id]?.passphrase)
        assertEquals(2, engine.validateCalls)
        assertEquals(2, engine.deriveCalls)
    }

    @Test
    fun restoreExistingSeedDoesNotInsertAgain() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val remote = FakeEthereumRemoteDataSource(balanceWei = "12345")
        val repo = createRepository(engine = engine, store = store, remote = remote)

        repo.createWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets().first().single().id
        repo.refreshBalance(id)
        val validateAfterCreate = engine.validateCalls
        val deriveAfterCreate = engine.deriveCalls
        val savedAfterCreate = store.saved.size

        repo.restoreWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = null)

        val wallets = repo.observeWallets().first()
        assertEquals(1, wallets.size)
        assertEquals(id, wallets.single().id)
        assertEquals("12345", wallets.single().balanceWei)
        assertEquals(savedAfterCreate, store.saved.size)
        assertEquals(validateAfterCreate + 1, engine.validateCalls)
        assertEquals(deriveAfterCreate + 1, engine.deriveCalls)
    }

    @Test
    fun restoreDifferentPassphraseCreatesAnotherWallet() = runTest {
        val engine = FakeEthereumKeyEngine(
            passphraseAddresses = mapOf("other-pass" to "0x2222222222222222222222222222222222222222"),
        )
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(engine = engine, store = store)

        repo.restoreWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        repo.restoreWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = "other-pass")

        val wallets = repo.observeWallets().first()
        assertEquals(2, wallets.size)
        assertEquals(
            setOf(DEFAULT_ADDRESS, "0x2222222222222222222222222222222222222222"),
            wallets.map { it.address }.toSet(),
        )
        assertEquals(2, store.saved.size)
    }

    @Test
    fun restoreDifferentNetworkCreatesAnotherWallet() = runTest {
        val store = FakeEthereumMnemonicStore()
        val networkStore = FakeSelectedEthereumNetworkStore()
        val repo = createRepository(store = store, networkStore = networkStore)

        repo.restoreWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        repo.restoreWallet(EthereumNetwork.MAINNET, VALID_WORDS, passphrase = null)

        assertEquals(2, store.saved.size)
        networkStore.setNetwork(EthereumNetwork.SEPOLIA)
        assertEquals(1, repo.observeWallets().first().size)
        networkStore.setNetwork(EthereumNetwork.MAINNET)
        assertEquals(1, repo.observeWallets().first().size)
    }

    @Test
    fun restoreRejectsInvalidMnemonic() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(engine = engine, store = store)

        try {
            repo.restoreWallet(EthereumNetwork.SEPOLIA, listOf("not", "valid"), null)
            error("expected InvalidEthereumMnemonicException")
        } catch (_: InvalidEthereumMnemonicException) {
        }

        assertTrue(repo.observeWallets().first().isEmpty())
        assertTrue(store.saved.isEmpty())
        assertEquals(0, engine.deriveCalls)
    }

    @Test
    fun refreshBalancePersistsWeiFromRemote() = runTest {
        val walletDao = FakeEthereumWalletDao()
        val remote = FakeEthereumRemoteDataSource(balanceWei = "1000000000000000000")
        val timeProvider = FakeTimeProvider(nowMillis = 1_700_000_000_000L)
        val repo = createRepository(
            walletDao = walletDao,
            remote = remote,
            timeProvider = timeProvider,
        )
        repo.createWallet(EthereumNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val walletId = repo.observeWallets().first().single().id

        repo.refreshBalance(walletId)

        val wallet = repo.observeWallet(walletId).first()
        assertEquals("1000000000000000000", wallet?.balanceWei)
        assertEquals(1_700_000_000_000L, wallet?.balanceUpdatedAtMillis)
        assertEquals(1, remote.balanceCalls)
    }

    private fun createRepository(
        engine: FakeEthereumKeyEngine = FakeEthereumKeyEngine(),
        store: FakeEthereumMnemonicStore = FakeEthereumMnemonicStore(),
        walletDao: FakeEthereumWalletDao = FakeEthereumWalletDao(),
        networkStore: FakeSelectedEthereumNetworkStore = FakeSelectedEthereumNetworkStore(),
        remote: FakeEthereumRemoteDataSource = FakeEthereumRemoteDataSource(),
        timeProvider: FakeTimeProvider = FakeTimeProvider(),
    ) = EthereumWalletRepositoryImpl(
        keyEngine = engine,
        mnemonicStore = store,
        walletDao = walletDao,
        selectedEthereumNetworkStore = networkStore,
        remote = remote,
        timeProvider = timeProvider,
    )
}

private val VALID_WORDS = List(11) { "abandon" } + "about"
private const val DEFAULT_ADDRESS = "0x1111111111111111111111111111111111111111"

private class FakeEthereumKeyEngine(
    private val passphraseAddresses: Map<String, String> = emptyMap(),
) : EthereumKeyEngine {
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
        val address = passphrase?.let { passphraseAddresses[it] } ?: DEFAULT_ADDRESS
        return EthereumReceiveAddress(address = address, index = 0)
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

    override fun observeById(id: String): Flow<EthereumWalletEntity?> =
        items.map { rows -> rows.find { it.id == id } }

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

    override suspend fun updateBalance(
        id: String,
        balanceWei: String,
        updatedAtMillis: Long,
    ) {
        items.update { rows ->
            rows.map { row ->
                if (row.id == id) {
                    row.copy(
                        balanceWei = balanceWei,
                        balanceUpdatedAtMillis = updatedAtMillis,
                    )
                } else {
                    row
                }
            }
        }
    }
}

private class FakeEthereumRemoteDataSource(
    private val balanceWei: String = "0",
) : EthereumRemoteDataSource {
    var balanceCalls = 0

    override suspend fun getAddressBalance(
        network: EthereumNetwork,
        address: String,
    ): EthereumAddressBalance {
        balanceCalls += 1
        return EthereumAddressBalance(balanceWei = balanceWei)
    }
}

private class FakeTimeProvider(
    private val nowMillis: Long = 0L,
) : TimeProvider {
    override fun nowMillis(): Long = nowMillis
}
