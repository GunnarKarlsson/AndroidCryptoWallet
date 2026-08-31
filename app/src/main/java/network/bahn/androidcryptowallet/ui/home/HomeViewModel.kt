package network.bahn.androidcryptowallet.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.repository.PortfolioRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    catalogReadiness: WalletCatalogReadiness,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private var hasEntered = false

    val uiState: StateFlow<HomeUiState> = combine(
        portfolioRepository.observeHoldings(),
        catalogReadiness.observeReady(),
        isRefreshing,
    ) { holdings, ready, refreshing ->
        HomeUiState(
            holdings = if (ready) holdings else emptyList(),
            assetCount = if (ready) holdings.size else 0,
            totalFiatFormatted = null,
            isLoading = !ready,
            isRefreshing = refreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onEnter() {
        if (hasEntered) return
        hasEntered = true
        refresh()
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.update { true }
            try {
                portfolioRepository.refreshAllBalances()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep cached balances visible; refresh is best-effort on home.
            } finally {
                isRefreshing.update { false }
            }
        }
    }
}
