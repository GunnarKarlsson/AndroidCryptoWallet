package network.bahn.androidcryptowallet.ui.ethereum.details

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
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EthereumWalletDetailsViewModelTest {
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
    fun onEnterLoadsCachedTransactionsWithoutNetworkFetch() = runTest {
        val cached = EthereumTransactionPage(
            transactions = listOf(TX),
            nextCursor = null,
            hasMore = true,
        )
        val repo = FakeEthDetailsWalletRepository(cachedPage = cached)
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onEnter()

        assertEquals(listOf(TX), viewModel.uiState.value.transactions)
        assertFalse(viewModel.uiState.value.isLoadingTransactions)
        assertFalse(viewModel.uiState.value.hasMoreTransactions)
        assertEquals(0, repo.getTransactionsCalls)
        job.cancel()
    }

    @Test
    fun onEnterFetchesWhenNoCache() = runTest {
        val repo = FakeEthDetailsWalletRepository(
            networkPage = EthereumTransactionPage(
                transactions = listOf(TX),
                nextCursor = null,
                hasMore = false,
            ),
        )
        val viewModel = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onEnter()

        assertEquals(1, repo.getTransactionsCalls)
        assertEquals(listOf(TX), viewModel.uiState.value.transactions)
        job.cancel()
    }

    @Test
    fun onEnterSkipsBalanceFetchWhenCachedIncludingZero() = runTest {
        val repo = FakeEthDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(balanceWei = "0"),
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
        val repo = FakeEthDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(
                    balanceWei = null,
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
        val repo = FakeEthDetailsWalletRepository(
            wallet = MutableStateFlow(
                WALLET.copy(balanceWei = "0"),
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
    fun onDeleteClickShowsConfirmDialog() = runTest {
        val viewModel = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onDeleteClick()

        assertEquals(true, viewModel.uiState.value.showDeleteConfirmDialog)
        job.cancel()
    }

    @Test
    fun onDismissDeleteConfirmHidesDialog() = runTest {
        val viewModel = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        viewModel.onDeleteClick()

        viewModel.onDismissDeleteConfirm()

        assertEquals(false, viewModel.uiState.value.showDeleteConfirmDialog)
        job.cancel()
    }

    @Test
    fun onConfirmDeleteCallsRepoAndEmitsWalletDeleted() = runTest {
        val repo = FakeEthDetailsWalletRepository()
        val viewModel = createViewModel(repo)
        val events = mutableListOf<EthereumWalletDetailsEvent>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onDeleteClick()

        viewModel.onConfirmDelete()

        assertEquals(listOf(WALLET.id), repo.deleteWalletCalls)
        assertEquals(listOf(EthereumWalletDetailsEvent.WalletDeleted), events)
        assertEquals(false, viewModel.uiState.value.showDeleteConfirmDialog)
        collectJob.cancel()
        eventsJob.cancel()
    }

    @Test
    fun onConfirmDeleteFailureSurfacesError() = runTest {
        val repo = FakeEthDetailsWalletRepository(
            deleteError = IllegalStateException("boom"),
        )
        val viewModel = createViewModel(repo)
        val events = mutableListOf<EthereumWalletDetailsEvent>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        val eventsJob = backgroundScope.launch(Dispatchers.Unconfined) {
            viewModel.events.collect { events += it }
        }
        viewModel.onDeleteClick()

        viewModel.onConfirmDelete()

        assertEquals(listOf(WALLET.id), repo.deleteWalletCalls)
        assertEquals(emptyList<EthereumWalletDetailsEvent>(), events)
        assertEquals(false, viewModel.uiState.value.isDeleting)
        assertEquals(false, viewModel.uiState.value.showDeleteConfirmDialog)
        assertEquals("boom", viewModel.uiState.value.errorMessage)
        collectJob.cancel()
        eventsJob.cancel()
    }

    private fun createViewModel(
        repo: FakeEthDetailsWalletRepository = FakeEthDetailsWalletRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("walletId" to WALLET.id)),
    ) = EthereumWalletDetailsViewModel(
        savedStateHandle = savedStateHandle,
        walletRepository = repo,
    )
}

private val WALLET = EthereumWallet(
    id = "wallet-1",
    network = EthereumNetwork.SEPOLIA,
    address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
    balanceWei = "1000000000000000000",
    balanceUpdatedAtMillis = 1_700_000_000_000L,
)

private val TX = network.bahn.androidcryptowallet.domain.model.EthereumTransactionSummary(
    hash = "0xabc",
    confirmed = true,
    blockTimeSeconds = 1_700_000_000L,
    netWei = "1000000000000000000",
    feeWei = "21000000000000",
)

private class FakeEthDetailsWalletRepository(
    private val wallet: MutableStateFlow<EthereumWallet?> = MutableStateFlow(WALLET),
    private val deleteError: Exception? = null,
    private val cachedPage: EthereumTransactionPage? = null,
    private val networkPage: EthereumTransactionPage = EthereumTransactionPage(
        transactions = emptyList(),
        nextCursor = null,
        hasMore = false,
    ),
) : EthereumWalletRepository {
    var refreshBalanceCalls = 0
    var getTransactionsCalls = 0
    val deleteWalletCalls = mutableListOf<String>()

    override fun observeWallets(): Flow<List<EthereumWallet>> = emptyFlow()
    override fun observeWallet(id: String): Flow<EthereumWallet?> = wallet
    override fun generateMnemonic() = error("unused")

    override suspend fun createWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun restoreWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) = error("unused")

    override suspend fun refreshBalance(walletId: String) {
        refreshBalanceCalls++
    }

    override suspend fun deleteWallet(walletId: String) {
        deleteWalletCalls += walletId
        if (deleteError != null) throw deleteError
        wallet.value = null
    }

    override suspend fun renameWallet(walletId: String, name: String?) = error("unused")

    override suspend fun getCachedTransactions(walletId: String): EthereumTransactionPage? = cachedPage

    override suspend fun getTransactions(
        walletId: String,
        afterCursor: EthereumTransactionPaginationCursor?,
    ): EthereumTransactionPage {
        getTransactionsCalls++
        return networkPage
    }
}
