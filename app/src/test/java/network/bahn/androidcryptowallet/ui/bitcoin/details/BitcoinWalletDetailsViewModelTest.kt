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
    fun refreshReplacesTransactionList() = runTest {
        val repo = FakeDetailsWalletRepository(
            firstPage = pageOf(TX_ONE, hasMore = true, lastConfirmedTxid = TX_ONE.txid),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onEnter()
        repo.firstPage = pageOf(TX_TWO)
        viewModel.onRefresh()

        assertEquals(listOf(TX_TWO), viewModel.uiState.value.transactions)
        assertFalse(viewModel.uiState.value.hasMoreTransactions)
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

    private fun createViewModel(
        repo: FakeDetailsWalletRepository = FakeDetailsWalletRepository(),
    ) = BitcoinWalletDetailsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
        observeBitcoinWallet = ObserveBitcoinWalletUseCase(repo),
        refreshBitcoinWalletBalance = RefreshBitcoinWalletBalanceUseCase(repo),
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
    var transactionsError: Exception? = null,
) : BitcoinWalletRepository {
    val txCursors = mutableListOf<String?>()

    override fun observeWallets(): Flow<List<BitcoinWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<BitcoinWallet?> = wallet
    override fun generateMnemonic() = error("unused")
    override suspend fun createWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) = Unit

    override suspend fun getTransactions(
        walletId: String,
        afterTxid: String?,
    ): BitcoinTransactionPage {
        transactionsError?.let { throw it }
        txCursors += afterTxid
        return if (afterTxid == null) firstPage else nextPage
    }
}
