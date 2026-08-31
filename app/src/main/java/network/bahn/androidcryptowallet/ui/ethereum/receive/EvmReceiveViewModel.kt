package network.bahn.androidcryptowallet.ui.ethereum.receive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bahn.androidcryptowallet.domain.model.EvmPaymentUri
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository
import network.bahn.androidcryptowallet.ui.navigation.EvmReceiveRoute
import javax.inject.Inject

@HiltViewModel
class EvmReceiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    walletRepository: EvmWalletRepository,
) : ViewModel() {
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<EvmReceiveRoute>().walletId

    val uiState: StateFlow<EvmReceiveUiState> = walletRepository.observeWallet(walletId)
        .map { wallet ->
            if (wallet == null) {
                EvmReceiveUiState()
            } else {
                val address = wallet.address.takeIf { it.isNotBlank() }
                EvmReceiveUiState(
                    address = address,
                    networkLabel = wallet.network.label,
                    family = wallet.network.family,
                    paymentUri = address?.let {
                        EvmPaymentUri.fromAddress(it, wallet.network)
                    },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EvmReceiveUiState(),
        )
}
