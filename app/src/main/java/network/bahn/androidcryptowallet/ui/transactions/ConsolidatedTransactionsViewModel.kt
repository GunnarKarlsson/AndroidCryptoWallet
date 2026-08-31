package network.bahn.androidcryptowallet.ui.transactions

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
import network.bahn.androidcryptowallet.data.local.prefs.WalletNetworkModeStore
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
import network.bahn.androidcryptowallet.domain.repository.ConsolidatedTransactionRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import javax.inject.Inject

@HiltViewModel
class ConsolidatedTransactionsViewModel @Inject constructor(
    private val consolidatedTransactionRepository: ConsolidatedTransactionRepository,
    private val walletNetworkModeStore: WalletNetworkModeStore,
    catalogReadiness: WalletCatalogReadiness,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<ConsolidatedTransactionsUiState> = combine(
        consolidatedTransactionRepository.observeTransactions(),
        walletNetworkModeStore.observeMode(),
        catalogReadiness.observeReady(),
        isRefreshing,
    ) { transactions, networkMode, catalogReady, refreshing ->
        ConsolidatedTransactionsUiState(
            transactions = transactions,
            networkMode = networkMode,
            isLoading = !catalogReady,
            isRefreshing = refreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConsolidatedTransactionsUiState(),
    )

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
                consolidatedTransactionRepository.refreshAllTransactions()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep cached transactions visible; refresh is best-effort.
            } finally {
                isRefreshing.update { false }
            }
        }
    }
}
