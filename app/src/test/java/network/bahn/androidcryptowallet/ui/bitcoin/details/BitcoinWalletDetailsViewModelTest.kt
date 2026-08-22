package network.bahn.androidcryptowallet.ui.bitcoin.details

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionSummary
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.domain.usecase.GetCachedBitcoinWalletTransactionsUseCase
import network.bahn.androidcryptowallet.domain.usecase.LoadBitcoinWalletTransactionsUseCase
import network.bahn.androidcryptowallet.domain.usecase.ObserveBitcoinWalletUseCase
import network.bahn.androidcryptowallet.domain.usecase.RefreshBitcoinWalletBalanceUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BitcoinWalletDetailsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoadingTransactions() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoadingTransactions)
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
    }

    @Test
    fun onEnterSkipsBalanceFetchWhenCachedIncludingZero() = runTest {
        val repo = FakeDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(
                    confirmedBalanceSatoshis = 0L,
                    unconfirmedBalanceSatoshis = 0L,
                ),
            ),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onEnter()

        assertEquals(0, repo.refreshBalanceCalls)
        job.cancel()
    }

    @Test
    fun onEnterFetchesBalanceWhenNeverCached() = runTest {
        val repo = FakeDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(
                    confirmedBalanceSatoshis = null,
                    unconfirmedBalanceSatoshis = null,
                    balanceUpdatedAtMillis = null,
                ),
            ),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onEnter()

        assertEquals(1, repo.refreshBalanceCalls)
        job.cancel()
    }

    @Test
    fun toolbarRefreshFetchesBalanceEvenWhenCachedZero() = runTest {
        val repo = FakeDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(
                    confirmedBalanceSatoshis = 0L,
                    unconfirmedBalanceSatoshis = 0L,
                ),
            ),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onEnter()
        assertEquals(0, repo.refreshBalanceCalls)

        viewModel.onRefresh()

        assertEquals(1, repo.refreshBalanceCalls)
        job.cancel()
    }

    @Test
    fun onEnterUsesCachedTransactionsWithoutNetwork() = runTest {
        val repo = FakeDetailsWalletRepository(cachedPage = pageOf(TX_ONE))
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onEnter()

        val latest = viewModel.uiState.value
        assertFalse(latest.isLoadingTransactions)
        assertEquals(listOf(TX_ONE), latest.transactions)
        assertTrue(repo.txCursors.isEmpty())
        assertFalse(latest.hasMoreTransactions)
        job.cancel()
    }

    @Test
    fun toolbarRefreshDoesNotFetchTransactions() = runTest {
        val repo = FakeDetailsWalletRepository(firstPage = pageOf(TX_ONE))
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onEnter()
        assertEquals(listOf(null as String?), repo.txCursors)

        viewModel.onRefresh()

        assertEquals(listOf(null as String?), repo.txCursors)
        assertEquals(listOf(TX_ONE), viewModel.uiState.value.transactions)
        job.cancel()
    }

    @Test
    fun onEnterShowsFirstPage() = runTest {
        val repo = FakeDetailsWalletRepository(firstPage = pageOf(TX_ONE))
        val viewModel = createViewModel(repo)
        val states = mutableListOf<BitcoinWalletDetailsUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        viewModel.onEnter()

        val latest = states.last()
        assertFalse(latest.isLoadingTransactions)
        assertEquals(listOf(TX_ONE), latest.transactions)
        assertFalse(latest.hasMoreTransactions)
        job.cancel()
    }

    @Test
    fun onEnterShowsEmptyWhenNoTransactions() = runTest {
        val viewModel = createViewModel(FakeDetailsWalletRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onEnter()

        val latest = viewModel.uiState.value
        assertFalse(latest.isLoadingTransactions)
        assertTrue(latest.transactions.isEmpty())
        assertNull(latest.transactionsErrorMessage)
        job.cancel()
    }

    @Test
    fun loadMoreAppendsNextPage() = runTest {
        val repo = FakeDetailsWalletRepository(
            firstPage = pageOf(TX_ONE, hasMore = true, lastConfirmedTxid = TX_ONE.txid),
            nextPage = pageOf(TX_TWO),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onEnter()
        viewModel.onLoadMore()

        assertEquals(listOf(TX_ONE, TX_TWO), viewModel.uiState.value.transactions)
        assertEquals(listOf(null, TX_ONE.txid), repo.txCursors)
        assertFalse(viewModel.uiState.value.hasMoreTransactions)
        job.cancel()
    }

    @Test
    fun refreshTransactionsReplacesListFromNetwork() = runTest {
        val repo = FakeDetailsWalletRepository(
            cachedPage = pageOf(TX_ONE, hasMore = true, lastConfirmedTxid = TX_ONE.txid),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onEnter()
        repo.firstPage = pageOf(TX_TWO)
        viewModel.onRefreshTransactions()

        assertEquals(listOf(TX_TWO), viewModel.uiState.value.transactions)
        assertFalse(viewModel.uiState.value.hasMoreTransactions)
        assertEquals(listOf(null), repo.txCursors)
        job.cancel()
    }

    @Test
    fun firstPageErrorDoesNotClearBalance() = runTest {
        val wallet = MutableStateFlow<BitcoinWallet?>(WALLET)
        val repo = FakeDetailsWalletRepository(
            wallet = wallet,
            transactionsError = IllegalStateException("txs down"),
        )
        val viewModel = createViewModel(repo)
        val states = mutableListOf<BitcoinWalletDetailsUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        viewModel.onEnter()

        val latest = states.last()
        assertEquals(WALLET, latest.wallet)
        assertEquals(12_345L, latest.confirmedBalanceSatoshis)
        assertEquals("txs down", latest.transactionsErrorMessage)
        assertNull(latest.errorMessage)
        job.cancel()
    }

    @Test
    fun reloadWalletFlagForceRefreshesBalanceAndTransactions() = runTest {
        val repo = FakeDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(
                    confirmedBalanceSatoshis = 0L,
                    unconfirmedBalanceSatoshis = 0L,
                ),
            ),
            cachedPage = pageOf(TX_ONE),
            firstPage = pageOf(TX_TWO),
        )
        val viewModel = createViewModel(
            repo = repo,
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "walletId" to WALLET.id,
                    BitcoinWalletDetailsViewModel.RELOAD_WALLET_KEY to true,
                ),
            ),
        )
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        assertEquals(1, repo.refreshBalanceCalls)
        assertEquals(listOf(null), repo.txCursors)
        assertEquals(listOf(TX_TWO), viewModel.uiState.value.transactions)
        job.cancel()
    }

    @Test
    fun onEnterDoesNotReloadAgainAfterFirstVisit() = runTest {
        val repo = FakeDetailsWalletRepository(firstPage = pageOf(TX_ONE))
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onEnter()
        assertEquals(listOf(null as String?), repo.txCursors)
        viewModel.onEnter()
        assertEquals(listOf(null as String?), repo.txCursors)
        job.cancel()
    }

    private fun createViewModel(
        repo: FakeDetailsWalletRepository = FakeDetailsWalletRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
    ) = BitcoinWalletDetailsViewModel(
        savedStateHandle = savedStateHandle,
        observeBitcoinWallet = ObserveBitcoinWalletUseCase(repo),
        refreshBitcoinWalletBalance = RefreshBitcoinWalletBalanceUseCase(repo),
        getCachedBitcoinWalletTransactions = GetCachedBitcoinWalletTransactionsUseCase(repo),
        loadBitcoinWalletTransactions = LoadBitcoinWalletTransactionsUseCase(repo),
    )
}

private val WALLET = BitcoinWallet(
    id = "wallet-1",
    network = BitcoinNetwork.TESTNET4,
    receiveAddress = "tb1qrestore",
    confirmedBalanceSatoshis = 12_345L,
    unconfirmedBalanceSatoshis = 0L,
    balanceUpdatedAtMillis = 1_700_000_000_000L,
)

private val TX_ONE = BitcoinTransactionSummary(
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

private fun pageOf(
    vararg transactions: BitcoinTransactionSummary,
    hasMore: Boolean = false,
    lastConfirmedTxid: String? = transactions.lastOrNull { it.confirmed }?.txid,
) = BitcoinTransactionPage(
    transactions = transactions.toList(),
    lastConfirmedTxid = lastConfirmedTxid,
    hasMore = hasMore,
)

private class FakeDetailsWalletRepository(
    private val wallet: MutableStateFlow<BitcoinWallet?> = MutableStateFlow(WALLET),
    var firstPage: BitcoinTransactionPage = pageOf(),
    var nextPage: BitcoinTransactionPage = pageOf(),
    var cachedPage: BitcoinTransactionPage? = null,
    var transactionsError: Exception? = null,
) : BitcoinWalletRepository {
    val txCursors = mutableListOf<String?>()
    var refreshBalanceCalls = 0

    override fun observeWallets(): Flow<List<BitcoinWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<BitcoinWallet?> = wallet
    override fun generateMnemonic() = error("unused")
    override suspend fun createWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun restoreWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) {
        refreshBalanceCalls++
    }

    override suspend fun renameWallet(walletId: String, name: String?) = error("unused")

    override suspend fun getCachedTransactions(walletId: String): BitcoinTransactionPage? = cachedPage

    override suspend fun getTransactions(
        walletId: String,
        afterTxid: String?,
    ): BitcoinTransactionPage {
        transactionsError?.let { throw it }
        txCursors += afterTxid
        val page = if (afterTxid == null) firstPage else nextPage
        cachedPage = if (afterTxid == null) {
            page
        } else {
            val existing = cachedPage?.transactions.orEmpty()
            page.copy(transactions = existing + page.transactions)
        }
        return page
    }

    override fun isValidAddress(network: BitcoinNetwork, address: String) = error("unused")

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
    ) = error("unused")
}
