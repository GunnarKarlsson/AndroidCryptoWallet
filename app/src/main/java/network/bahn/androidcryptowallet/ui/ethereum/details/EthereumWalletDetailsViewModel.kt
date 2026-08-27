package network.bahn.androidcryptowallet.ui.ethereum.details

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
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import network.bahn.androidcryptowallet.ui.navigation.EthereumWalletDetailsRoute
import javax.inject.Inject

@HiltViewModel
class EthereumWalletDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: EthereumWalletRepository,
) : ViewModel() {
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<EthereumWalletDetailsRoute>().walletId
    private val isRefreshing = MutableStateFlow(false)
    private val showDeleteConfirmDialog = MutableStateFlow(false)
    private val isDeleting = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val eventsChannel = Channel<EthereumWalletDetailsEvent>(Channel.BUFFERED)
    private var deleteJob: Job? = null
    private var hasEntered = false

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<EthereumWalletDetailsUiState> = combine(
        walletRepository.observeWallet(walletId),
        isRefreshing,
        showDeleteConfirmDialog,
        isDeleting,
        errorMessage,
    ) { wallet, refreshing, showDelete, deleting, error ->
        EthereumWalletDetailsUiState(
            wallet = wallet,
            isRefreshing = refreshing,
            showDeleteConfirmDialog = showDelete,
            isDeleting = deleting,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EthereumWalletDetailsUiState(),
    )

    fun onEnter() {
        if (hasEntered) return
        hasEntered = true
        refreshBalance(force = false)
    }

    fun onRefresh() {
        refreshBalance(force = true)
    }

    fun onDeleteClick() {
        if (isDeleting.value) return
        errorMessage.value = null
        showDeleteConfirmDialog.value = true
    }

    fun onDismissDeleteConfirm() {
        if (isDeleting.value) return
        showDeleteConfirmDialog.value = false
    }

    fun onConfirmDelete() {
        if (deleteJob?.isActive == true) return
        deleteJob = viewModelScope.launch {
            errorMessage.value = null
            isDeleting.value = true
            try {
                walletRepository.deleteWallet(walletId)
                showDeleteConfirmDialog.value = false
                eventsChannel.send(EthereumWalletDetailsEvent.WalletDeleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
                isDeleting.value = false
                showDeleteConfirmDialog.value = false
                errorMessage.value = e.message?.takeIf { it.isNotBlank() }
                    ?: DELETE_FAILED
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

    companion object {
        private const val TAG = "EthWalletDetails"
        private const val DELETE_FAILED = "Could not delete wallet"
    }
}
