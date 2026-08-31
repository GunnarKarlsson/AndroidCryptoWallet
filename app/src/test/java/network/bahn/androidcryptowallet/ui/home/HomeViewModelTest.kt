package network.bahn.androidcryptowallet.ui.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bahn.androidcryptowallet.domain.model.PortfolioHolding
import network.bahn.androidcryptowallet.domain.model.PortfolioHoldingDestination
import network.bahn.androidcryptowallet.domain.repository.PortfolioRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoading() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.holdings.isEmpty())
    }

    @Test
    fun showsHoldingsAfterCatalogReady() = runTest {
        val ready = MutableStateFlow(false)
        val holdings = listOf(
            PortfolioHolding(
                destination = PortfolioHoldingDestination.Bitcoin,
                headline = "Bitcoin (BTC)",
                nativeSymbol = "BTC",
                balanceSatoshis = 100L,
            ),
        )
        val viewModel = createViewModel(holdings = holdings, ready = ready)
        val states = mutableListOf<HomeUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        ready.value = true

        val latest = states.last()
        assertFalse(latest.isLoading)
        assertEquals(1, latest.assetCount)
        assertEquals("Bitcoin (BTC)", latest.holdings.single().headline)
        job.cancel()
    }

    @Test
    fun refreshCallsRepository() = runTest {
        val portfolioRepository = FakePortfolioRepository()
        val viewModel = createViewModel(portfolioRepository = portfolioRepository, ready = MutableStateFlow(true))

        viewModel.refresh()

        assertEquals(1, portfolioRepository.refreshCalls)
    }

    private fun createViewModel(
        holdings: List<PortfolioHolding> = emptyList(),
        ready: MutableStateFlow<Boolean> = MutableStateFlow(true),
        portfolioRepository: FakePortfolioRepository = FakePortfolioRepository(holdings),
    ): HomeViewModel = HomeViewModel(
        portfolioRepository = portfolioRepository,
        catalogReadiness = FakeWalletCatalogReadiness(ready),
    )
}

private class FakePortfolioRepository(
    private val holdings: List<PortfolioHolding> = emptyList(),
) : PortfolioRepository {
    var refreshCalls = 0

    override fun observeHoldings(): Flow<List<PortfolioHolding>> = flowOf(holdings)

    override suspend fun refreshAllBalances() {
        refreshCalls++
    }
}

private class FakeWalletCatalogReadiness(
    private val ready: MutableStateFlow<Boolean>,
) : WalletCatalogReadiness {
    override fun observeReady(): Flow<Boolean> = ready

    override suspend fun initialize() = Unit
}
