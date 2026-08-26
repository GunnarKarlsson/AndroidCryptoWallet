package network.bahn.androidcryptowallet.ui.ethereum.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val walletId = savedStateHandle.toRoute<EthereumWalletDetailsRoute>().walletId
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private var hasEntered = false

    val uiState: StateFlow<EthereumWalletDetailsUiState> = combine(
        walletRepository.observeWallet(walletId),
        isRefreshing,
        errorMessage,
    ) { wallet, refreshing, error ->
        EthereumWalletDetailsUiState(
            wallet = wallet,
            isRefreshing = refreshing,
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
    }
}
