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
import network.bahn.androidcryptowallet.domain.usecase.ObserveBitcoinWalletsUseCase
import network.bahn.androidcryptowallet.domain.usecase.ObserveSelectedBitcoinNetworkUseCase
import network.bahn.androidcryptowallet.domain.usecase.ObserveWalletCatalogReadyUseCase
import network.bahn.androidcryptowallet.domain.usecase.SetBitcoinNetworkUseCase
import javax.inject.Inject

@HiltViewModel
class BitcoinWalletListViewModel @Inject constructor(
    observeSelectedBitcoinNetwork: ObserveSelectedBitcoinNetworkUseCase,
    observeBitcoinWallets: ObserveBitcoinWalletsUseCase,
    observeWalletCatalogReady: ObserveWalletCatalogReadyUseCase,
    private val setBitcoinNetwork: SetBitcoinNetworkUseCase,
) : ViewModel() {
    val uiState: StateFlow<BitcoinWalletListUiState> = combine(
        observeSelectedBitcoinNetwork(),
        observeBitcoinWallets(),
        observeWalletCatalogReady(),
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
            setBitcoinNetwork(network)
        }
    }
}
