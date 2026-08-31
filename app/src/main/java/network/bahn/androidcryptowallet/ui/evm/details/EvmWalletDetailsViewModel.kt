package network.bahn.androidcryptowallet.ui.evm.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPage
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EvmTransactionSummary
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository
import network.bahn.androidcryptowallet.ui.navigation.EvmWalletDetailsRoute
import javax.inject.Inject

@HiltViewModel
class EvmWalletDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: EvmWalletRepository,
) : ViewModel() {
    private val routeHandle = savedStateHandle
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<EvmWalletDetailsRoute>().walletId
    private val isRefreshing = MutableStateFlow(false)
    private val deleteState = MutableStateFlow(DeleteState())
    private val errorMessage = MutableStateFlow<String?>(null)
    private val txLoadState = MutableStateFlow(TxLoadState())
    private val eventsChannel = Channel<EvmWalletDetailsEvent>(Channel.BUFFERED)
    private var nextCursor: EvmTransactionPaginationCursor? = null
    private var firstPageJob: Job? = null
    private var loadMoreJob: Job? = null
    private var deleteJob: Job? = null
    private var hasEntered = false

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<EvmWalletDetailsUiState> = combine(
        walletRepository.observeWallet(walletId),
        isRefreshing,
        deleteState,
        errorMessage,
        txLoadState,
    ) { wallet, refreshing, delete, error, txs ->
        EvmWalletDetailsUiState(
            wallet = wallet,
            isRefreshing = refreshing,
            showDeleteConfirmDialog = delete.showConfirmDialog,
            isDeleting = delete.isDeleting,
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
        initialValue = EvmWalletDetailsUiState(isLoadingTransactions = true),
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

    fun onDeleteClick() {
        if (deleteState.value.isDeleting) return
        errorMessage.value = null
        deleteState.update { it.copy(showConfirmDialog = true) }
    }

    fun onDismissDeleteConfirm() {
        if (deleteState.value.isDeleting) return
        deleteState.update { it.copy(showConfirmDialog = false) }
    }

    fun onConfirmDelete() {
        if (deleteJob?.isActive == true) return
        deleteJob = viewModelScope.launch {
            errorMessage.value = null
            deleteState.update { it.copy(isDeleting = true) }
            try {
                walletRepository.deleteWallet(walletId)
                deleteState.update { it.copy(showConfirmDialog = false) }
                eventsChannel.send(EvmWalletDetailsEvent.WalletDeleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
                deleteState.value = DeleteState()
                errorMessage.value = e.message?.takeIf { it.isNotBlank() }
                    ?: DELETE_FAILED
            }
        }
    }

    fun onRefreshTransactions() {
        loadFirstPageFromNetwork(showFullSpinner = txLoadState.value.transactions.isEmpty())
    }

    fun onLoadMore() {
        val state = txLoadState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMore) return
        val cursor = nextCursor ?: return
        if (loadMoreJob?.isActive == true) return
        loadMoreJob = viewModelScope.launch {
            txLoadState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            try {
                val page = walletRepository.getTransactions(walletId, cursor)
                nextCursor = page.nextCursor
                txLoadState.update { current ->
                    val existing = current.transactions.map { it.hash }.toSet()
                    current.copy(
                        transactions = current.transactions +
                            page.transactions.filter { it.hash !in existing },
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
                val wallet = walletRepository.observeWallet(walletId).first()
                if (wallet?.balanceWei != null) return@launch
            }
            errorMessage.value = null
            isRefreshing.value = true
            try {
                walletRepository.refreshBalance(walletId)
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
            nextCursor = null
            txLoadState.value = TxLoadState(isLoading = true)
            try {
                val cached = walletRepository.getCachedTransactions(walletId)
                if (cached != null) {
                    nextCursor = cached.nextCursor
                    txLoadState.value = TxLoadState(
                        transactions = cached.transactions,
                        isLoading = false,
                        hasMore = false,
                    )
                    return@launch
                }
                applyFirstPage(walletRepository.getTransactions(walletId))
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
            nextCursor = null
            txLoadState.update {
                it.copy(
                    isLoading = showFullSpinner,
                    isRefreshing = !showFullSpinner,
                    errorMessage = null,
                )
            }
            try {
                applyFirstPage(walletRepository.getTransactions(walletId))
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

    private fun applyFirstPage(page: EvmTransactionPage) {
        nextCursor = page.nextCursor
        txLoadState.value = TxLoadState(
            transactions = page.transactions,
            isLoading = false,
            isRefreshing = false,
            hasMore = page.hasMore,
        )
    }

    private data class TxLoadState(
        val transactions: List<EvmTransactionSummary> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        val errorMessage: String? = null,
    )

    private data class DeleteState(
        val showConfirmDialog: Boolean = false,
        val isDeleting: Boolean = false,
    )

    companion object {
        private const val TAG = "EthWalletDetails"
        private const val DELETE_FAILED = "Could not delete wallet"
        const val RELOAD_WALLET_KEY = "reload_wallet"
    }
}
