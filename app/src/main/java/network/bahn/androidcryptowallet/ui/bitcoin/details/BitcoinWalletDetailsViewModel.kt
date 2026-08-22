package network.bahn.androidcryptowallet.ui.bitcoin.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionSummary
import network.bahn.androidcryptowallet.domain.usecase.GetCachedBitcoinWalletTransactionsUseCase
import network.bahn.androidcryptowallet.domain.usecase.LoadBitcoinWalletTransactionsUseCase
import network.bahn.androidcryptowallet.domain.usecase.ObserveBitcoinWalletUseCase
import network.bahn.androidcryptowallet.domain.usecase.RefreshBitcoinWalletBalanceUseCase
import network.bahn.androidcryptowallet.ui.navigation.BitcoinWalletDetailsRoute
import javax.inject.Inject

@HiltViewModel
class BitcoinWalletDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeBitcoinWallet: ObserveBitcoinWalletUseCase,
    private val refreshBitcoinWalletBalance: RefreshBitcoinWalletBalanceUseCase,
    private val getCachedBitcoinWalletTransactions: GetCachedBitcoinWalletTransactionsUseCase,
    private val loadBitcoinWalletTransactions: LoadBitcoinWalletTransactionsUseCase,
) : ViewModel() {
    private val routeHandle = savedStateHandle
    private val walletId = savedStateHandle.toRoute<BitcoinWalletDetailsRoute>().walletId
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val txLoadState = MutableStateFlow(TxLoadState())
    private var lastConfirmedTxid: String? = null
    private var firstPageJob: Job? = null
    private var loadMoreJob: Job? = null
    private var hasEntered = false

    val uiState: StateFlow<BitcoinWalletDetailsUiState> = combine(
        observeBitcoinWallet(walletId),
        isRefreshing,
        errorMessage,
        txLoadState,
    ) { wallet, refreshing, error, txs ->
        BitcoinWalletDetailsUiState(
            wallet = wallet,
            isRefreshing = refreshing,
            errorMessage = error,
            transactions = txs.transactions,
            isLoadingTransactions = txs.isLoading,
            isRefreshingTransactions = txs.isRefreshing,
            isLoadingMoreTransactions = txs.isLoadingMore,
            hasMoreTransactions = txs.hasMore,
            transactionsErrorMessage = txs.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BitcoinWalletDetailsUiState(),
    )

    init {
        viewModelScope.launch {
            routeHandle.getStateFlow(RELOAD_WALLET_KEY, false).collect { reload ->
                if (!reload) return@collect
                routeHandle[RELOAD_WALLET_KEY] = false
                onReturnFromSend()
            }
        }
    }

    fun onEnter() {
        if (hasEntered) return
        hasEntered = true
        refreshBalance(force = false)
        loadCachedOrFetch()
    }

    fun onReturnFromSend() {
        refreshBalance(force = true)
        loadFirstPageFromNetwork(showFullSpinner = txLoadState.value.transactions.isEmpty())
    }

    fun onRefresh() {
        refreshBalance(force = true)
    }

    fun onRefreshTransactions() {
        loadFirstPageFromNetwork(showFullSpinner = txLoadState.value.transactions.isEmpty())
    }

    fun onLoadMore() {
        val state = txLoadState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMore) return
        val cursor = lastConfirmedTxid ?: return
        if (loadMoreJob?.isActive == true) return
        loadMoreJob = viewModelScope.launch {
            txLoadState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            try {
                val page = loadBitcoinWalletTransactions(walletId, cursor)
                lastConfirmedTxid = page.lastConfirmedTxid ?: lastConfirmedTxid
                txLoadState.update { current ->
                    val existing = current.transactions.map { it.txid }.toSet()
                    current.copy(
                        transactions = current.transactions +
                            page.transactions.filter { it.txid !in existing },
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Load more transactions failed", e)
                txLoadState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = e.message?.takeIf { it.isNotBlank() }
                            ?: "Could not load transactions",
                    )
                }
            }
        }
    }

    private fun refreshBalance(force: Boolean) {
        if (isRefreshing.value) return
        viewModelScope.launch {
            if (!force) {
                val wallet = observeBitcoinWallet(walletId).first()
                if (wallet?.confirmedBalanceSatoshis != null) return@launch
            }
            errorMessage.value = null
            isRefreshing.value = true
            try {
                refreshBitcoinWalletBalance(walletId)
            } catch (e: Exception) {
                Log.e(TAG, "Balance refresh failed", e)
                errorMessage.value = e.message?.takeIf { it.isNotBlank() }
                    ?: "Could not refresh balance"
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private fun loadCachedOrFetch() {
        firstPageJob?.cancel()
        loadMoreJob?.cancel()
        firstPageJob = viewModelScope.launch {
            lastConfirmedTxid = null
            txLoadState.value = TxLoadState(isLoading = true)
            try {
                val cached = getCachedBitcoinWalletTransactions(walletId)
                if (cached != null) {
                    lastConfirmedTxid = cached.lastConfirmedTxid
                    txLoadState.value = TxLoadState(
                        transactions = cached.transactions,
                        isLoading = false,
                        hasMore = false,
                    )
                    return@launch
                }
                applyFirstPage(loadBitcoinWalletTransactions(walletId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Load transactions failed", e)
                txLoadState.value = TxLoadState(
                    isLoading = false,
                    errorMessage = e.message?.takeIf { it.isNotBlank() }
                        ?: "Could not load transactions",
                )
            }
        }
    }

    private fun loadFirstPageFromNetwork(showFullSpinner: Boolean) {
        firstPageJob?.cancel()
        loadMoreJob?.cancel()
        firstPageJob = viewModelScope.launch {
            lastConfirmedTxid = null
            txLoadState.update {
                it.copy(
                    isLoading = showFullSpinner,
                    isRefreshing = !showFullSpinner,
                    errorMessage = null,
                )
            }
            try {
                applyFirstPage(loadBitcoinWalletTransactions(walletId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Refresh transactions failed", e)
                txLoadState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.message?.takeIf { it.isNotBlank() }
                            ?: "Could not load transactions",
                    )
                }
            }
        }
    }

    private fun applyFirstPage(page: BitcoinTransactionPage) {
        lastConfirmedTxid = page.lastConfirmedTxid
        txLoadState.value = TxLoadState(
            transactions = page.transactions,
            isLoading = false,
            isRefreshing = false,
            hasMore = page.hasMore,
        )
    }

    private data class TxLoadState(
        val transactions: List<BitcoinTransactionSummary> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        val errorMessage: String? = null,
    )

    companion object {
        const val RELOAD_WALLET_KEY = "reload_wallet"
        private const val TAG = "WalletDetails"
    }
}
