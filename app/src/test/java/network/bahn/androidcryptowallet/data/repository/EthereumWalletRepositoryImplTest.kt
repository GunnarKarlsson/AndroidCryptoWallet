package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.data.local.db.EthereumTransactionDao
import network.bahn.androidcryptowallet.data.local.db.EthereumTransactionEntity
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletEntity
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletTxCacheEntity
import network.bahn.androidcryptowallet.data.local.db.toJson
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEvmNetworkStore
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.data.remote.EthereumRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.blockscout.EthereumTransactionRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.EthereumKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.EthereumAddressBalance
import network.bahn.androidcryptowallet.domain.model.EthereumFeeData
import network.bahn.androidcryptowallet.domain.model.EthereumGasPreset
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumReceiveAddress
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionSummary
import network.bahn.androidcryptowallet.domain.model.InvalidEthereumMnemonicException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigInteger

class EthereumWalletRepositoryImplTest {
    @Test
    fun createWritesWalletForChosenNetworkOnly() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val networkStore = FakeSelectedEvmNetworkStore()
        val repo = createRepository(engine = engine, store = store, networkStore = networkStore)

        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)

        val wallets = repo.observeWallets(EvmFamily.ETHEREUM).first()
        assertEquals(1, wallets.size)
        assertEquals(EvmNetwork.SEPOLIA, wallets.single().network)
        assertEquals(DEFAULT_ADDRESS, wallets.single().address)
        assertEquals(VALID_WORDS.joinToString(" "), store.saved[wallets.single().id]?.mnemonic)
        assertEquals(null, store.saved[wallets.single().id]?.passphrase)
        assertEquals(EvmNetwork.SEPOLIA, store.saved[wallets.single().id]?.network)
        assertEquals(1, engine.validateCalls)
        assertEquals(1, engine.deriveCalls)

        networkStore.setNetwork(EvmFamily.ETHEREUM, EvmNetwork.MAINNET)
        assertTrue(repo.observeWallets(EvmFamily.ETHEREUM).first().isEmpty())
    }

    @Test
    fun createRejectsInvalidMnemonic() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(engine = engine, store = store)

        try {
            repo.createWallet(EvmNetwork.SEPOLIA, listOf("not", "valid"), null)
            error("expected InvalidEthereumMnemonicException")
        } catch (_: InvalidEthereumMnemonicException) {
        }

        assertTrue(repo.observeWallets(EvmFamily.ETHEREUM).first().isEmpty())
        assertTrue(store.saved.isEmpty())
        assertEquals(0, engine.deriveCalls)
    }

    @Test
    fun createPersistsPassphrase() = runTest {
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(store = store)

        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = "secret")

        val id = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id
        assertEquals("secret", store.saved[id]?.passphrase)
    }

    @Test
    fun restoreWritesWalletForChosenNetwork() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val networkStore = FakeSelectedEvmNetworkStore()
        val repo = createRepository(engine = engine, store = store, networkStore = networkStore)

        repo.restoreWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)

        val wallets = repo.observeWallets(EvmFamily.ETHEREUM).first()
        assertEquals(1, wallets.size)
        assertEquals(EvmNetwork.SEPOLIA, wallets.single().network)
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

        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id
        repo.refreshBalance(id)
        val validateAfterCreate = engine.validateCalls
        val deriveAfterCreate = engine.deriveCalls
        val savedAfterCreate = store.saved.size

        repo.restoreWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)

        val wallets = repo.observeWallets(EvmFamily.ETHEREUM).first()
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

        repo.restoreWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        repo.restoreWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = "other-pass")

        val wallets = repo.observeWallets(EvmFamily.ETHEREUM).first()
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
        val networkStore = FakeSelectedEvmNetworkStore()
        val repo = createRepository(store = store, networkStore = networkStore)

        repo.restoreWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        repo.restoreWallet(EvmNetwork.MAINNET, VALID_WORDS, passphrase = null)

        assertEquals(2, store.saved.size)
        networkStore.setNetwork(EvmFamily.ETHEREUM, EvmNetwork.SEPOLIA)
        assertEquals(1, repo.observeWallets(EvmFamily.ETHEREUM).first().size)
        networkStore.setNetwork(EvmFamily.ETHEREUM, EvmNetwork.MAINNET)
        assertEquals(1, repo.observeWallets(EvmFamily.ETHEREUM).first().size)
    }

    @Test
    fun observeWallets_filtersByFamilyAndSelectedNetwork() = runTest {
        val networkStore = FakeSelectedEvmNetworkStore()
        val repo = createRepository(networkStore = networkStore)

        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        repo.createWallet(EvmNetwork.BSC_TESTNET, VALID_WORDS, passphrase = null)

        networkStore.setNetwork(EvmFamily.ETHEREUM, EvmNetwork.SEPOLIA)
        networkStore.setNetwork(EvmFamily.BSC, EvmNetwork.BSC_TESTNET)

        assertEquals(1, repo.observeWallets(EvmFamily.ETHEREUM).first().size)
        assertEquals(EvmNetwork.SEPOLIA, repo.observeWallets(EvmFamily.ETHEREUM).first().single().network)
        assertEquals(1, repo.observeWallets(EvmFamily.BSC).first().size)
        assertEquals(EvmNetwork.BSC_TESTNET, repo.observeWallets(EvmFamily.BSC).first().single().network)
    }

    @Test
    fun restoreRejectsInvalidMnemonic() = runTest {
        val engine = FakeEthereumKeyEngine()
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(engine = engine, store = store)

        try {
            repo.restoreWallet(EvmNetwork.SEPOLIA, listOf("not", "valid"), null)
            error("expected InvalidEthereumMnemonicException")
        } catch (_: InvalidEthereumMnemonicException) {
        }

        assertTrue(repo.observeWallets(EvmFamily.ETHEREUM).first().isEmpty())
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
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val walletId = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id

        repo.refreshBalance(walletId)

        val wallet = repo.observeWallet(walletId).first()
        assertEquals("1000000000000000000", wallet?.balanceWei)
        assertEquals(1_700_000_000_000L, wallet?.balanceUpdatedAtMillis)
        assertEquals(1, remote.balanceCalls)
    }

    @Test
    fun deleteRemovesMnemonicAndRoomRow() = runTest {
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(store = store)
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = "secret")
        val walletId = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id

        repo.deleteWallet(walletId)

        assertTrue(repo.observeWallets(EvmFamily.ETHEREUM).first().isEmpty())
        assertTrue(store.saved.isEmpty())
        assertEquals(null, repo.observeWallet(walletId).first())
    }

    @Test
    fun deleteUnknownIdIsNoOp() = runTest {
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(store = store)
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val existingId = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id

        repo.deleteWallet("missing-id")

        assertEquals(1, repo.observeWallets(EvmFamily.ETHEREUM).first().size)
        assertEquals(existingId, repo.observeWallets(EvmFamily.ETHEREUM).first().single().id)
        assertEquals(1, store.saved.size)
    }

    @Test
    fun renameWalletPersistsTrimmedNameWithoutTouchingRemote() = runTest {
        val remote = FakeEthereumRemoteDataSource()
        val engine = FakeEthereumKeyEngine()
        val repo = createRepository(engine = engine, remote = remote)

        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id
        repo.renameWallet(id, "  Savings  ")

        assertEquals("Savings", repo.observeWallet(id).first()?.name)
        assertEquals(0, remote.balanceCalls)
        assertEquals(1, engine.deriveCalls)
    }

    @Test
    fun renameWalletBlankBecomesNull() = runTest {
        val repo = createRepository()
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id
        repo.renameWallet(id, "Savings")
        repo.renameWallet(id, "   ")

        assertEquals(null, repo.observeWallet(id).first()?.name)
    }

    @Test
    fun renameWalletMissingThrowsWithoutRemote() = runTest {
        val remote = FakeEthereumRemoteDataSource()
        val engine = FakeEthereumKeyEngine()
        val repo = createRepository(engine = engine, remote = remote)
        try {
            repo.renameWallet("missing", "Savings")
            error("expected Wallet not found")
        } catch (e: IllegalStateException) {
            assertEquals("Wallet not found", e.message)
        }
        assertEquals(0, remote.balanceCalls)
        assertEquals(0, engine.deriveCalls)
    }

    @Test
    fun getCachedTransactionsReturnsNullWhenNeverFetched() = runTest {
        val repo = createRepository()
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val walletId = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id

        assertEquals(null, repo.getCachedTransactions(walletId))
    }

    @Test
    fun getTransactionsPersistsFirstPageAndReturnsCache() = runTest {
        val txRemote = FakeEthereumTransactionRemoteDataSource(
            firstPage = EthereumTransactionPage(
                transactions = listOf(TX_SUMMARY),
                nextCursor = EthereumTransactionPaginationCursor(
                    blockNumber = 1L,
                    index = 2,
                    hash = "0xabc",
                    insertedAt = null,
                    value = null,
                    fee = null,
                    itemsCount = 50,
                ),
                hasMore = true,
            ),
        )
        val transactionDao = FakeEthereumTransactionDao()
        val timeProvider = FakeTimeProvider(nowMillis = 1_700_000_000_000L)
        val json = Json { ignoreUnknownKeys = true }
        val repo = createRepository(
            transactionDao = transactionDao,
            transactionRemote = txRemote,
            timeProvider = timeProvider,
            json = json,
        )
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val walletId = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id

        val page = repo.getTransactions(walletId)

        assertEquals(listOf(TX_SUMMARY), page.transactions)
        assertEquals(1, txRemote.txCalls.size)
        assertEquals(null, txRemote.txCalls.single().afterCursor)
        val cached = repo.getCachedTransactions(walletId)
        assertEquals(listOf(TX_SUMMARY), cached?.transactions)
        assertEquals(1, transactionDao.transactions.size)
        assertEquals(walletId, transactionDao.transactions.single().walletId)
        assertEquals(TX_SUMMARY.hash, transactionDao.transactions.single().hash)
        assertEquals(true, transactionDao.cache?.hasMore)
    }

    @Test
    fun getTransactionsAppendUsesCursor() = runTest {
        val cursor = EthereumTransactionPaginationCursor(
            blockNumber = 1L,
            index = 2,
            hash = "0xabc",
            insertedAt = null,
            value = null,
            fee = null,
            itemsCount = 50,
        )
        val txRemote = FakeEthereumTransactionRemoteDataSource(
            firstPage = EthereumTransactionPage(
                transactions = listOf(TX_SUMMARY),
                nextCursor = cursor,
                hasMore = true,
            ),
            nextPage = EthereumTransactionPage(
                transactions = listOf(TX_SUMMARY_2),
                nextCursor = null,
                hasMore = false,
            ),
        )
        val transactionDao = FakeEthereumTransactionDao()
        val repo = createRepository(
            transactionDao = transactionDao,
            transactionRemote = txRemote,
        )
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val walletId = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id
        repo.getTransactions(walletId)

        val page = repo.getTransactions(walletId, cursor)

        assertEquals(listOf(TX_SUMMARY_2), page.transactions)
        assertEquals(cursor, txRemote.txCalls.last().afterCursor)
        assertEquals(2, transactionDao.transactions.size)
    }

    @Test
    fun sendBroadcastsSignedTransaction() = runTest {
        val remote = FakeEthereumRemoteDataSource(
            balanceWei = "1000000000000000000",
        )
        val store = FakeEthereumMnemonicStore()
        val repo = createRepository(store = store, remote = remote)
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val walletId = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id
        // Balance is set via remote on refresh so send can check funds.
        repo.refreshBalance(walletId)

        val txHash = repo.send(
            walletId = walletId,
            recipientAddress = "0x2222222222222222222222222222222222222222",
            amountWei = BigInteger("100000000000000000"),
            gasPreset = EthereumGasPreset.Normal,
        )

        assertEquals("0xbroadcast", txHash)
        assertEquals(1, remote.sendRawCalls)
    }

    @Test
    fun sendRejectsInsufficientFunds() = runTest {
        val remote = FakeEthereumRemoteDataSource(balanceWei = "1000")
        val repo = createRepository(remote = remote)
        repo.createWallet(EvmNetwork.SEPOLIA, VALID_WORDS, passphrase = null)
        val walletId = repo.observeWallets(EvmFamily.ETHEREUM).first().single().id
        repo.refreshBalance(walletId)

        try {
            repo.send(
                walletId = walletId,
                recipientAddress = "0x2222222222222222222222222222222222222222",
                amountWei = BigInteger("100000000000000000"),
                gasPreset = EthereumGasPreset.Normal,
            )
            fail("expected insufficient funds")
        } catch (e: IllegalStateException) {
            assertEquals("Insufficient funds", e.message)
        }
        assertEquals(0, remote.sendRawCalls)
    }

    private fun createRepository(
        engine: FakeEthereumKeyEngine = FakeEthereumKeyEngine(),
        store: FakeEthereumMnemonicStore = FakeEthereumMnemonicStore(),
        walletDao: FakeEthereumWalletDao = FakeEthereumWalletDao(),
        transactionDao: FakeEthereumTransactionDao = FakeEthereumTransactionDao(),
        networkStore: FakeSelectedEvmNetworkStore = FakeSelectedEvmNetworkStore(),
        remote: FakeEthereumRemoteDataSource = FakeEthereumRemoteDataSource(),
        transactionRemote: FakeEthereumTransactionRemoteDataSource = FakeEthereumTransactionRemoteDataSource(),
        timeProvider: FakeTimeProvider = FakeTimeProvider(),
        json: Json = Json { ignoreUnknownKeys = true },
    ) = EthereumWalletRepositoryImpl(
        keyEngine = engine,
        mnemonicStore = store,
        walletDao = walletDao,
        transactionDao = transactionDao,
        selectedEvmNetworkStore = networkStore,
        remote = remote,
        transactionRemote = transactionRemote,
        timeProvider = timeProvider,
        json = json,
    )
}

private val VALID_WORDS = List(11) { "abandon" } + "about"
private const val DEFAULT_ADDRESS = "0x1111111111111111111111111111111111111111"

private val TX_SUMMARY = EthereumTransactionSummary(
    hash = "0xabc",
    confirmed = true,
    blockTimeSeconds = 1_700_000_000L,
    netWei = "1000000000000000000",
    feeWei = "21000000000000",
)

private val TX_SUMMARY_2 = TX_SUMMARY.copy(hash = "0xdef")

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

    override fun isValidAddress(address: String): Boolean =
        address.startsWith("0x") && address.length == 42

    override fun buildAndSignSend(
        mnemonicWords: List<String>,
        passphrase: String?,
        chainId: Long,
        to: String,
        valueWei: java.math.BigInteger,
        nonce: Long,
        gasLimit: Long,
        maxPriorityFeePerGasWei: java.math.BigInteger,
        maxFeePerGasWei: java.math.BigInteger,
    ): String = "0xsignedraw"
}

private class FakeEthereumMnemonicStore : EthereumMnemonicStore {
    data class Saved(
        val mnemonic: String,
        val passphrase: String?,
        val network: EvmNetwork,
    )

    val saved = mutableMapOf<String, Saved>()

    override fun save(
        walletId: String,
        mnemonic: String,
        passphrase: String?,
        network: EvmNetwork,
    ) {
        saved[walletId] = Saved(mnemonic, passphrase, network)
    }

    override fun listHdWalletIds(): List<String> = saved.keys.sorted()

    override fun delete(walletId: String) {
        saved.remove(walletId)
    }

    override fun loadNetwork(walletId: String): EvmNetwork? = saved[walletId]?.network

    override fun loadMnemonic(walletId: String): String? = saved[walletId]?.mnemonic

    override fun loadPassphrase(walletId: String): String? = saved[walletId]?.passphrase
}

private class FakeSelectedEvmNetworkStore(
    initialByFamily: Map<EvmFamily, EvmNetwork> = mapOf(
        EvmFamily.ETHEREUM to EvmNetwork.SEPOLIA,
        EvmFamily.BSC to EvmNetwork.BSC_TESTNET,
    ),
) : SelectedEvmNetworkStore {
    private val networks = EvmFamily.entries.associateWith { family ->
        MutableStateFlow(
            initialByFamily[family] ?: EvmNetwork.networksFor(family).first(),
        )
    }

    override fun selectedNetwork(family: EvmFamily): Flow<EvmNetwork> =
        networks.getValue(family)

    override suspend fun setNetwork(family: EvmFamily, network: EvmNetwork) {
        require(network.family == family)
        networks.getValue(family).value = network
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

    override suspend fun deleteById(id: String) {
        items.update { rows -> rows.filter { it.id != id } }
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

    override suspend fun updateName(id: String, name: String?) {
        items.update { rows ->
            rows.map { row ->
                if (row.id == id) row.copy(name = name) else row
            }
        }
    }
}

private class FakeEthereumRemoteDataSource(
    private val balanceWei: String = "0",
    private val feeData: EthereumFeeData = EthereumFeeData(
        baseFeePerGasWei = "1000000000",
        suggestedPriorityFeePerGasWei = "1500000000",
    ),
    private val nonce: Long = 0L,
    private val estimatedGas: Long = 21_000L,
    private val broadcastTxHash: String = "0xbroadcast",
) : EthereumRemoteDataSource {
    var balanceCalls = 0
    var sendRawCalls = 0

    override suspend fun getAddressBalance(
        network: EvmNetwork,
        address: String,
    ): EthereumAddressBalance {
        balanceCalls += 1
        return EthereumAddressBalance(balanceWei = balanceWei)
    }

    override suspend fun getTransactionCount(
        network: EvmNetwork,
        address: String,
    ): Long = nonce

    override suspend fun estimateGas(
        network: EvmNetwork,
        from: String,
        to: String,
        valueWei: java.math.BigInteger,
    ): Long = estimatedGas

    override suspend fun getFeeData(network: EvmNetwork): EthereumFeeData = feeData

    override suspend fun sendRawTransaction(
        network: EvmNetwork,
        signedRawHex: String,
    ): String {
        sendRawCalls += 1
        return broadcastTxHash
    }
}

private class FakeTimeProvider(
    private val nowMillis: Long = 0L,
) : TimeProvider {
    override fun nowMillis(): Long = nowMillis
}

private class FakeEthereumTransactionDao : EthereumTransactionDao {
    val transactions = mutableListOf<EthereumTransactionEntity>()
    var cache: EthereumWalletTxCacheEntity? = null

    override suspend fun listByWalletId(walletId: String): List<EthereumTransactionEntity> =
        transactions.filter { it.walletId == walletId }.sortedBy { it.sortIndex }

    override suspend fun maxSortIndex(walletId: String): Int =
        transactions.filter { it.walletId == walletId }.maxOfOrNull { it.sortIndex } ?: -1

    override suspend fun cacheForWallet(walletId: String): EthereumWalletTxCacheEntity? =
        cache?.takeIf { it.walletId == walletId }

    override suspend fun upsertTransactions(entities: List<EthereumTransactionEntity>) {
        entities.forEach { entity ->
            transactions.removeAll { it.walletId == entity.walletId && it.hash == entity.hash }
            transactions += entity
        }
    }

    override suspend fun deleteByWalletId(walletId: String) {
        transactions.removeAll { it.walletId == walletId }
    }

    override suspend fun upsertCache(entity: EthereumWalletTxCacheEntity) {
        cache = entity
    }

    override suspend fun replaceWalletTransactions(
        walletId: String,
        transactions: List<EthereumTransactionEntity>,
        cache: EthereumWalletTxCacheEntity,
    ) {
        deleteByWalletId(walletId)
        upsertTransactions(transactions)
        upsertCache(cache)
    }

    override suspend fun appendWalletTransactions(
        transactions: List<EthereumTransactionEntity>,
        cache: EthereumWalletTxCacheEntity,
    ) {
        upsertTransactions(transactions)
        upsertCache(cache)
    }
}

private class FakeEthereumTransactionRemoteDataSource(
    private val firstPage: EthereumTransactionPage = EthereumTransactionPage(
        transactions = emptyList(),
        nextCursor = null,
        hasMore = false,
    ),
    private val nextPage: EthereumTransactionPage = firstPage,
) : EthereumTransactionRemoteDataSource {
    data class TxCall(
        val network: EvmNetwork,
        val address: String,
        val afterCursor: EthereumTransactionPaginationCursor?,
    )

    val txCalls = mutableListOf<TxCall>()

    override suspend fun getAddressTransactions(
        network: EvmNetwork,
        address: String,
        afterCursor: EthereumTransactionPaginationCursor?,
    ): EthereumTransactionPage {
        txCalls += TxCall(network, address, afterCursor)
        return if (afterCursor == null) firstPage else nextPage
    }
}
