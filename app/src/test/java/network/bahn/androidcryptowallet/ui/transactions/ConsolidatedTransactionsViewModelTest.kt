package network.bahn.androidcryptowallet.ui.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bahn.androidcryptowallet.domain.model.ConsolidatedTransaction
import network.bahn.androidcryptowallet.data.local.prefs.WalletNetworkModeStore
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
import network.bahn.androidcryptowallet.domain.repository.ConsolidatedTransactionRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsolidatedTransactionsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsLoadingUntilCatalogReady() = runTest {
        val viewModel = createViewModel(ready = MutableStateFlow(false))
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun showsCachedTransactionsFromRepository() = runTest {
        val transactions = listOf(
            ConsolidatedTransaction.Bitcoin(
                id = "btc:abc:wallet-1",
                walletId = "wallet-1",
                walletName = "Savings",
                chainLabel = "Bitcoin Testnet4 (BTC)",
                timestampSeconds = 1_700_000_000L,
                confirmed = true,
                isIncoming = true,
                txReference = "abc",
                netSatoshis = 100L,
            ),
        )
        val viewModel = createViewModel(transactions = transactions, ready = MutableStateFlow(true))
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun refreshCallsRepository() = runTest {
        val repository = FakeConsolidatedTransactionRepository()
        val viewModel = createViewModel(repository = repository, ready = MutableStateFlow(true))
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.refreshCalls)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    private fun createViewModel(
        transactions: List<ConsolidatedTransaction> = emptyList(),
        ready: MutableStateFlow<Boolean> = MutableStateFlow(true),
        repository: FakeConsolidatedTransactionRepository = FakeConsolidatedTransactionRepository(transactions),
    ): ConsolidatedTransactionsViewModel = ConsolidatedTransactionsViewModel(
        consolidatedTransactionRepository = repository,
        walletNetworkModeStore = FakeWalletNetworkModeStore(),
        catalogReadiness = FakeWalletCatalogReadiness(ready),
    )

    private class FakeWalletNetworkModeStore(
        private val mode: MutableStateFlow<WalletNetworkMode> = MutableStateFlow(WalletNetworkMode.TESTNET),
    ) : WalletNetworkModeStore {
        override fun observeMode(): Flow<WalletNetworkMode> = mode

        override suspend fun setMode(mode: WalletNetworkMode) {
            this.mode.value = mode
        }
    }

    private class FakeConsolidatedTransactionRepository(
        private val transactions: List<ConsolidatedTransaction> = emptyList(),
    ) : ConsolidatedTransactionRepository {
        var refreshCalls = 0

        override fun observeTransactions(): Flow<List<ConsolidatedTransaction>> = flowOf(transactions)

        override suspend fun refreshAllTransactions() {
            refreshCalls++
        }
    }

    private class FakeWalletCatalogReadiness(
        private val ready: MutableStateFlow<Boolean>,
    ) : WalletCatalogReadiness {
        override fun observeReady(): Flow<Boolean> = ready

        override suspend fun initialize() = Unit
    }
}
