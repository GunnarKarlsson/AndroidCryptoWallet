package network.bahn.androidcryptowallet.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.data.local.prefs.WalletNetworkModeStore
import network.bahn.androidcryptowallet.domain.model.HoldingsValueCalculator
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
import network.bahn.androidcryptowallet.domain.repository.AssetPriceRepository
import network.bahn.androidcryptowallet.domain.repository.PortfolioRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import network.bahn.androidcryptowallet.ui.util.StringUtils
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val assetPriceRepository: AssetPriceRepository,
    private val walletNetworkModeStore: WalletNetworkModeStore,
    catalogReadiness: WalletCatalogReadiness,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    /** Guards the one automatic refresh per app session (shell ViewModel lifetime). */
    private var hasAutoRefreshedThisSession = false

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            portfolioRepository.observeHoldings(),
            assetPriceRepository.observePrices(),
        ) { holdings, prices -> holdings to prices },
        catalogReadiness.observeReady(),
        walletNetworkModeStore.observeMode(),
        isRefreshing,
    ) { holdingsAndPrices, ready, networkMode, refreshing ->
        val (holdings, prices) = holdingsAndPrices
        val rows = holdings.map { holding ->
            HomeHoldingRow(
                holding = holding,
                fiatFormatted = HoldingsValueCalculator.holdingUsdMicros(holding, prices)
                    ?.let(StringUtils::formatUsdMicros),
            )
        }
        HomeUiState(
            holdings = rows,
            assetCount = rows.size,
            totalFiatFormatted = HoldingsValueCalculator.totalUsdMicros(holdings, prices)
                ?.let(StringUtils::formatUsdMicros),
            networkMode = networkMode,
            isTotalLoading = refreshing,
            isHoldingsLoading = holdings.isEmpty() && (!ready || refreshing),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /** Refreshes balances once when the app session starts; manual [refresh] any time. */
    fun onEnter() {
        if (hasAutoRefreshedThisSession) return
        hasAutoRefreshedThisSession = true
        refresh()
    }

    fun setNetworkMode(mode: WalletNetworkMode) {
        viewModelScope.launch {
            walletNetworkModeStore.setMode(mode)
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.update { true }
            try {
                coroutineScope {
                    launch { portfolioRepository.refreshAllBalances() }
                    launch { assetPriceRepository.refreshPrices() }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep cached balances and prices visible; refresh is best-effort on home.
            } finally {
                isRefreshing.update { false }
            }
        }
    }
}
