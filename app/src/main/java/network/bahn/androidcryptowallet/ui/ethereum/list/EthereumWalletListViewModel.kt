package network.bahn.androidcryptowallet.ui.ethereum.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkStore
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import javax.inject.Inject

@HiltViewModel
class EthereumWalletListViewModel @Inject constructor(
    walletRepository: EthereumWalletRepository,
    private val selectedEthereumNetworkStore: SelectedEthereumNetworkStore,
    catalogReadiness: WalletCatalogReadiness,
) : ViewModel() {
    val uiState: StateFlow<EthereumWalletListUiState> = combine(
        selectedEthereumNetworkStore.selectedNetwork,
        walletRepository.observeWallets(),
        catalogReadiness.observeReady(),
    ) { network, wallets, ready ->
        EthereumWalletListUiState(
            selectedNetwork = network,
            wallets = wallets,
            isLoading = !ready,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EthereumWalletListUiState(),
    )

    fun onNetworkSelected(network: EvmNetwork) {
        viewModelScope.launch {
            selectedEthereumNetworkStore.setNetwork(network)
        }
    }
}
