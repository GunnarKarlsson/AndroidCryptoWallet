package network.bahn.androidcryptowallet.ui.bitcoin.details

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.usecase.ObserveBitcoinWalletUseCase
import network.bahn.androidcryptowallet.domain.usecase.RefreshBitcoinWalletBalanceUseCase
import network.bahn.androidcryptowallet.ui.navigation.BitcoinWalletDetailsRoute
import javax.inject.Inject

@HiltViewModel
class BitcoinWalletDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBitcoinWallet: ObserveBitcoinWalletUseCase,
    private val refreshBitcoinWalletBalance: RefreshBitcoinWalletBalanceUseCase,
) : ViewModel() {
    private val walletId = savedStateHandle.toRoute<BitcoinWalletDetailsRoute>().walletId
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BitcoinWalletDetailsUiState> = combine(
        observeBitcoinWallet(walletId),
        isRefreshing,
        errorMessage,
    ) { wallet, refreshing, error ->
        BitcoinWalletDetailsUiState(
            wallet = wallet,
            isRefreshing = refreshing,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BitcoinWalletDetailsUiState(),
    )

    fun onEnter() {
        refreshBalance()
    }

    fun onRefresh() {
        refreshBalance()
    }

    private fun refreshBalance() {
        if (isRefreshing.value) return
        viewModelScope.launch {
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

    private companion object {
        const val TAG = "Alchemy"
    }
}
