package network.bahn.androidcryptowallet.ui.bitcoin.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import javax.inject.Inject

@HiltViewModel
class BitcoinWalletListViewModel @Inject constructor(
    walletRepository: BitcoinWalletRepository,
    private val networkStatusRepository: BitcoinNetworkStatusRepository,
    catalogReadiness: WalletCatalogReadiness,
) : ViewModel() {
    val uiState: StateFlow<BitcoinWalletListUiState> = combine(
        networkStatusRepository.selectedNetwork(),
        walletRepository.observeWallets(),
        catalogReadiness.observeReady(),
    ) { network, wallets, ready ->
        BitcoinWalletListUiState(
            selectedNetwork = network,
            wallets = wallets,
            isLoading = !ready,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BitcoinWalletListUiState(),
    )

    fun onNetworkSelected(network: BitcoinNetwork) {
        viewModelScope.launch {
            networkStatusRepository.setNetwork(network)
        }
    }
}
