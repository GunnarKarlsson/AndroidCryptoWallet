package network.bahn.androidcryptowallet.ui.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bahn.androidcryptowallet.domain.model.PortfolioHolding
import network.bahn.androidcryptowallet.domain.model.PortfolioHoldingDestination
import network.bahn.androidcryptowallet.data.local.prefs.WalletNetworkModeStore
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
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
    fun initialStateShowsHoldingsLoadingWhenCatalogNotReadyAndNoHoldings() = runTest {
        val viewModel = createViewModel(ready = MutableStateFlow(false))
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isHoldingsLoading)
        assertTrue(viewModel.uiState.value.holdings.isEmpty())
    }

    @Test
    fun showsCachedHoldingsBeforeCatalogReady() = runTest {
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
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isHoldingsLoading)
        assertEquals(1, viewModel.uiState.value.assetCount)
        assertEquals("Bitcoin (BTC)", viewModel.uiState.value.holdings.single().headline)
    }

    @Test
    fun refreshKeepsCachedHoldingsVisible() = runTest {
        val holdings = listOf(
            PortfolioHolding(
                destination = PortfolioHoldingDestination.Bitcoin,
                headline = "Bitcoin (BTC)",
                nativeSymbol = "BTC",
                balanceSatoshis = 100L,
            ),
        )
        val viewModel = createViewModel(holdings = holdings, ready = MutableStateFlow(true))
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.holdings.size)
        assertFalse(viewModel.uiState.value.isTotalLoading)
    }

    @Test
    fun refreshSetsTotalLoadingWhileHoldingsStayVisible() = runTest {
        val portfolioRepository = SlowRefreshPortfolioRepository(
            holdings = listOf(
                PortfolioHolding(
                    destination = PortfolioHoldingDestination.Bitcoin,
                    headline = "Bitcoin (BTC)",
                    nativeSymbol = "BTC",
                    balanceSatoshis = 100L,
                ),
            ),
        )
        val viewModel = createViewModel(
            portfolioRepository = portfolioRepository,
            ready = MutableStateFlow(true),
        )
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.refresh()
        assertTrue(viewModel.uiState.value.isTotalLoading)
        assertFalse(viewModel.uiState.value.isHoldingsLoading)
        assertEquals(1, viewModel.uiState.value.holdings.size)

        portfolioRepository.completeRefresh()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isTotalLoading)
    }

    @Test
    fun refreshCallsRepository() = runTest {
        val portfolioRepository = FakePortfolioRepository()
        val viewModel = createViewModel(portfolioRepository = portfolioRepository, ready = MutableStateFlow(true))
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.refresh()

        assertEquals(1, portfolioRepository.refreshCalls)
    }

    @Test
    fun onEnterDoesNotAutoRefreshAgainAfterFirstVisit() = runTest {
        val portfolioRepository = FakePortfolioRepository()
        val viewModel = createViewModel(
            portfolioRepository = portfolioRepository,
            ready = MutableStateFlow(true),
        )
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onEnter()
        advanceUntilIdle()
        viewModel.onEnter()
        advanceUntilIdle()

        assertEquals(1, portfolioRepository.refreshCalls)
    }

    private fun createViewModel(
        holdings: List<PortfolioHolding> = emptyList(),
        ready: MutableStateFlow<Boolean> = MutableStateFlow(true),
        portfolioRepository: PortfolioRepository = FakePortfolioRepository(holdings),
    ): HomeViewModel = HomeViewModel(
        portfolioRepository = portfolioRepository,
        walletNetworkModeStore = FakeWalletNetworkModeStore(),
        catalogReadiness = FakeWalletCatalogReadiness(ready),
    )
}

private class FakeWalletNetworkModeStore(
    private val mode: MutableStateFlow<WalletNetworkMode> = MutableStateFlow(WalletNetworkMode.TESTNET),
) : WalletNetworkModeStore {
    override fun observeMode(): Flow<WalletNetworkMode> = mode

    override suspend fun setMode(mode: WalletNetworkMode) {
        this.mode.value = mode
    }
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

private class SlowRefreshPortfolioRepository(
    private val holdings: List<PortfolioHolding>,
) : PortfolioRepository {
    private val refreshGate = CompletableDeferred<Unit>()

    override fun observeHoldings(): Flow<List<PortfolioHolding>> = flowOf(holdings)

    override suspend fun refreshAllBalances() {
        refreshGate.await()
    }

    fun completeRefresh() {
        refreshGate.complete(Unit)
    }
}

private class FakeWalletCatalogReadiness(
    private val ready: MutableStateFlow<Boolean>,
) : WalletCatalogReadiness {
    override fun observeReady(): Flow<Boolean> = ready

    override suspend fun initialize() = Unit
}
