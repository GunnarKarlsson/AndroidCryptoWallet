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
import network.bahn.androidcryptowallet.domain.model.EthereumPaymentUri
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import network.bahn.androidcryptowallet.ui.navigation.EthereumReceiveRoute
import javax.inject.Inject

@HiltViewModel
class EthereumReceiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    walletRepository: EthereumWalletRepository,
) : ViewModel() {
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<EthereumReceiveRoute>().walletId

    val uiState: StateFlow<EthereumReceiveUiState> = walletRepository.observeWallet(walletId)
        .map { wallet ->
            if (wallet == null) {
                EthereumReceiveUiState()
            } else {
                val address = wallet.address.takeIf { it.isNotBlank() }
                EthereumReceiveUiState(
                    address = address,
                    networkLabel = wallet.network.label,
                    paymentUri = address?.let {
                        EthereumPaymentUri.fromAddress(it, wallet.network)
                    },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EthereumReceiveUiState(),
        )
}
