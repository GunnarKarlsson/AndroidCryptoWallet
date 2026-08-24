package network.bahn.androidcryptowallet.ui.bitcoin.receive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.ui.navigation.BitcoinReceiveRoute
import javax.inject.Inject

@HiltViewModel
class BitcoinReceiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    walletRepository: BitcoinWalletRepository,
) : ViewModel() {
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<BitcoinReceiveRoute>().walletId

    val uiState: StateFlow<BitcoinReceiveUiState> = walletRepository.observeWallet(walletId)
        .map { wallet ->
            BitcoinReceiveUiState(
                address = wallet?.receiveAddress,
                networkLabel = wallet?.network?.label,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BitcoinReceiveUiState(),
        )
}
