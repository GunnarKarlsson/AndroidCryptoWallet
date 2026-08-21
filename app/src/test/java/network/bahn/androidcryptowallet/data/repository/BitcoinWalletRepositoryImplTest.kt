package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.data.local.db.BitcoinTransactionDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinTransactionEntity
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletEntity
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletTxCacheEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.BitcoinAddressBalance
import network.bahn.androidcryptowallet.domain.model.BitcoinHdWalletPublic
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionSummary
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
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
        assertEquals(BitcoinWalletKind.HD, wallets.single().kind)
        assertEquals(VALID_WORDS.joinToString(" "), store.saved[wallets.single().id]?.mnemonic)
        assertEquals(null, store.saved[wallets.single().id]?.passphrase)
        assertEquals(BitcoinNetwork.TESTNET4, store.saved[wallets.single().id]?.public?.network)
        assertEquals(TESTNET_ADDRESS, store.saved[wallets.single().id]?.public?.receiveAddress)
        assertEquals(0, store.saved[wallets.single().id]?.public?.derivationIndex)
        assertEquals(BitcoinScriptType.BIP84, store.saved[wallets.single().id]?.public?.scriptType)
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

    @Test
    fun getTransactionsPassesNetworkAddressAndCursor() = runTest {
        val remote = FakeWalletBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)

        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets().first().single().id
        val page = repo.getTransactions(id, afterTxid = "cursor-txid")

        assertEquals(listOf(TX_TWO), page.transactions)
        assertEquals(TX_TWO.txid, page.lastConfirmedTxid)
        assertEquals(listOf(TESTNET_ADDRESS), remote.txAddresses)
        assertEquals(listOf(BitcoinNetwork.TESTNET4), remote.txNetworks)
        assertEquals(listOf("cursor-txid"), remote.txCursors)
    }

    @Test
    fun getCachedTransactionsIsNullBeforeFetch() = runTest {
        val repo = createRepository()
        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets().first().single().id

        assertEquals(null, repo.getCachedTransactions(id))
    }

    @Test
    fun getTransactionsPersistsForLaterCacheRead() = runTest {
        val remote = FakeWalletBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)
        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets().first().single().id

        repo.getTransactions(id)
        assertEquals(1, remote.txCursors.size)

        val cached = repo.getCachedTransactions(id)
        assertEquals(listOf(TX_SUMMARY), cached?.transactions)
        assertEquals(1, remote.txCursors.size)
    }

    @Test
    fun getTransactionsEmptyPageIsStillCached() = runTest {
        val remote = FakeWalletBitcoinRemoteDataSource(
            firstTxPage = BitcoinTransactionPage(
                transactions = emptyList(),
                lastConfirmedTxid = null,
                hasMore = false,
            ),
        )
        val repo = createRepository(remote = remote)
        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets().first().single().id

        repo.getTransactions(id)
        val cached = repo.getCachedTransactions(id)
        assertEquals(emptyList<BitcoinTransactionSummary>(), cached?.transactions)
        assertEquals(false, cached?.hasMore)
    }

    @Test
    fun getTransactionsFirstPageReplacesCache() = runTest {
        val remote = FakeWalletBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)
        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets().first().single().id

        repo.getTransactions(id)
        remote.firstTxPage = BitcoinTransactionPage(
            transactions = listOf(TX_TWO),
            lastConfirmedTxid = TX_TWO.txid,
            hasMore = false,
        )
        repo.getTransactions(id)

        assertEquals(listOf(TX_TWO), repo.getCachedTransactions(id)?.transactions)
    }

    @Test
    fun getTransactionsAppendsWhenCursorProvided() = runTest {
        val remote = FakeWalletBitcoinRemoteDataSource()
        val repo = createRepository(remote = remote)
        repo.createWallet(BitcoinNetwork.TESTNET4, VALID_WORDS, passphrase = null)
        val id = repo.observeWallets().first().single().id

        repo.getTransactions(id)
        repo.getTransactions(id, afterTxid = TX_SUMMARY.txid)

        assertEquals(listOf(TX_SUMMARY, TX_TWO), repo.getCachedTransactions(id)?.transactions)
        assertEquals(listOf(null, TX_SUMMARY.txid), remote.txCursors)
    }

    @Test
    fun getTransactionsFailsWhenWalletMissing() = runTest {
        val repo = createRepository()
        try {
            repo.getTransactions("missing")
            error("expected failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Wallet not found"))
        }
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
        transactionDao = FakeBitcoinTransactionDao(),
        selectedBitcoinNetworkStore = networkStore,
        remote = remote,
        timeProvider = TimeProvider { 1_700_000_000_000L },
    )
}

private val VALID_WORDS = List(12) { "abandon" }.dropLast(1) + "about"
private const val MAINNET_ADDRESS = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"
private const val TESTNET_ADDRESS = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34"
private val TX_SUMMARY = BitcoinTransactionSummary(
    txid = "txid-1",
    confirmed = true,
    blockTimeSeconds = 1_700_000_000L,
    netSatoshis = 1_000L,
    feeSatoshis = 10L,
)
private val TX_TWO = BitcoinTransactionSummary(
    txid = "txid-2",
    confirmed = true,
    blockTimeSeconds = 1_699_000_000L,
    netSatoshis = -500L,
    feeSatoshis = 20L,
)

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

private data class SavedHdWallet(
    val mnemonic: String,
    val passphrase: String?,
    val public: BitcoinHdWalletPublic,
)

private class FakeBitcoinMnemonicStore : BitcoinMnemonicStore {
    val saved = mutableMapOf<String, SavedHdWallet>()

    override fun save(
        mnemonic: String,
        passphrase: String?,
        public: BitcoinHdWalletPublic,
    ) {
        saved[public.id] = SavedHdWallet(mnemonic, passphrase, public)
    }

    override fun listHdWalletIds(): List<String> = saved.keys.sorted()

    override fun loadPublic(walletId: String): BitcoinHdWalletPublic? = saved[walletId]?.public
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

    override suspend fun insertIgnore(entity: BitcoinWalletEntity) {
        items.update { rows ->
            if (rows.any { it.id == entity.id }) rows else rows + entity
        }
    }

    override suspend fun mockWalletIds(): List<String> =
        items.value.filter { it.id.startsWith("mock:") }.map { it.id }

    override suspend fun deleteByIds(ids: List<String>) {
        val idSet = ids.toSet()
        items.update { rows -> rows.filter { it.id !in idSet } }
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

private class FakeBitcoinTransactionDao : BitcoinTransactionDao {
    private val items = mutableListOf<BitcoinTransactionEntity>()
    private val caches = mutableMapOf<String, BitcoinWalletTxCacheEntity>()

    override suspend fun listByWalletId(walletId: String): List<BitcoinTransactionEntity> =
        items.filter { it.walletId == walletId }.sortedBy { it.sortIndex }

    override suspend fun maxSortIndex(walletId: String): Int =
        items.filter { it.walletId == walletId }.maxOfOrNull { it.sortIndex } ?: -1

    override suspend fun cacheForWallet(walletId: String): BitcoinWalletTxCacheEntity? =
        caches[walletId]

    override suspend fun upsertTransactions(entities: List<BitcoinTransactionEntity>) {
        entities.forEach { entity ->
            items.removeAll { it.walletId == entity.walletId && it.txid == entity.txid }
            items += entity
        }
    }

    override suspend fun deleteByWalletId(walletId: String) {
        items.removeAll { it.walletId == walletId }
    }

    override suspend fun upsertCache(entity: BitcoinWalletTxCacheEntity) {
        caches[entity.walletId] = entity
    }
}

private class FakeWalletBitcoinRemoteDataSource(
    var firstTxPage: BitcoinTransactionPage = BitcoinTransactionPage(
        transactions = listOf(TX_SUMMARY),
        lastConfirmedTxid = TX_SUMMARY.txid,
        hasMore = false,
    ),
    var nextTxPage: BitcoinTransactionPage = BitcoinTransactionPage(
        transactions = listOf(TX_TWO),
        lastConfirmedTxid = TX_TWO.txid,
        hasMore = false,
    ),
) : BitcoinRemoteDataSource {
    val networks = mutableListOf<BitcoinNetwork>()
    val addresses = mutableListOf<String>()
    val txNetworks = mutableListOf<BitcoinNetwork>()
    val txAddresses = mutableListOf<String>()
    val txCursors = mutableListOf<String?>()

    override suspend fun getBlockCount(network: BitcoinNetwork): Long = error("unused")

    override suspend fun getAddressBalance(
        network: BitcoinNetwork,
        address: String,
    ): BitcoinAddressBalance {
        networks += network
        addresses += address
        return BitcoinAddressBalance(confirmedSatoshis = 12_345L, unconfirmedSatoshis = 100L)
    }

    override suspend fun getAddressTransactions(
        network: BitcoinNetwork,
        address: String,
        afterTxid: String?,
    ): BitcoinTransactionPage {
        txNetworks += network
        txAddresses += address
        txCursors += afterTxid
        return if (afterTxid == null) firstTxPage else nextTxPage
    }
}

private class FakeWalletSelectedBitcoinNetworkStore : SelectedBitcoinNetworkStore {
    override val selectedNetwork = MutableStateFlow(BitcoinNetwork.TESTNET4)

    override suspend fun setNetwork(network: BitcoinNetwork) {
        selectedNetwork.value = network
    }
}
