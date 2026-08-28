package network.bahn.androidcryptowallet.ui.ethereum.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEvmNetworkStore
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import network.bahn.androidcryptowallet.ui.navigation.EvmWalletListRoute
import javax.inject.Inject

@HiltViewModel
class EthereumWalletListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    walletRepository: EthereumWalletRepository,
    private val selectedEvmNetworkStore: SelectedEvmNetworkStore,
    catalogReadiness: WalletCatalogReadiness,
) : ViewModel() {
    private val family: EvmFamily =
        savedStateHandle.get<String>("family")?.let(EvmFamily::valueOf)
            ?: savedStateHandle.toRoute<EvmWalletListRoute>().family
    private val availableNetworks = EvmNetwork.networksFor(family)

    val uiState: StateFlow<EthereumWalletListUiState> = combine(
        selectedEvmNetworkStore.selectedNetwork(family),
        walletRepository.observeWallets(family),
        catalogReadiness.observeReady(),
    ) { network, wallets, ready ->
        EthereumWalletListUiState(
            family = family,
            availableNetworks = availableNetworks,
            selectedNetwork = network,
            wallets = wallets,
            isLoading = !ready,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EthereumWalletListUiState(
            family = family,
            availableNetworks = availableNetworks,
        ),
    )

    fun onNetworkSelected(network: EvmNetwork) {
        if (network.family != family) return
        viewModelScope.launch {
            selectedEvmNetworkStore.setNetwork(family, network)
        }
    }
}
