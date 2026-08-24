package network.bahn.androidcryptowallet.ui.ethereum.list

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import javax.inject.Inject

@HiltViewModel
class EthereumWalletListViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(EthereumWalletListUiState())
    val uiState: StateFlow<EthereumWalletListUiState> = _uiState.asStateFlow()

    fun onNetworkSelected(network: EthereumNetwork) {
        _uiState.update { it.copy(selectedNetwork = network) }
    }
}
