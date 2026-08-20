package network.bahn.androidcryptowallet.ui.bitcoin.status

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.usecase.ObserveBitcoinNetworkStatusUseCase
import network.bahn.androidcryptowallet.domain.usecase.ObserveSelectedBitcoinNetworkUseCase
import network.bahn.androidcryptowallet.domain.usecase.RefreshBitcoinBlockHeightUseCase
import network.bahn.androidcryptowallet.domain.usecase.SetBitcoinNetworkUseCase
import javax.inject.Inject

@HiltViewModel
class BitcoinNetworkStatusViewModel @Inject constructor(
    private val observeSelectedBitcoinNetwork: ObserveSelectedBitcoinNetworkUseCase,
    observeBitcoinNetworkStatus: ObserveBitcoinNetworkStatusUseCase,
    private val setBitcoinNetwork: SetBitcoinNetworkUseCase,
    private val refreshBlockHeight: RefreshBitcoinBlockHeightUseCase,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BitcoinNetworkStatusUiState> = combine(
        observeSelectedBitcoinNetwork(),
        observeBitcoinNetworkStatus(),
        isRefreshing,
        errorMessage,
    ) { network, status, refreshing, error ->
        BitcoinNetworkStatusUiState(
            selectedNetwork = network,
            blockHeight = status?.blockHeight,
            updatedAtMillis = status?.updatedAtMillis,
            isRefreshing = refreshing,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BitcoinNetworkStatusUiState(),
    )

    fun onEnter() {
        viewModelScope.launch {
            onRefresh(observeSelectedBitcoinNetwork().first())
        }
    }

    fun onNetworkSelected(network: BitcoinNetwork) {
        onRefresh(network)
    }

    fun onRefresh(network: BitcoinNetwork) {
        viewModelScope.launch {
            errorMessage.value = null
            isRefreshing.value = true
            try {
                setBitcoinNetwork(network)
                refreshBlockHeight()
            } catch (e: Exception) {
                Log.e(TAG, "Refresh failed for $network", e)
                errorMessage.value = e.message?.takeIf { it.isNotBlank() }
                    ?: "Could not refresh block height"
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private companion object {
        const val TAG = "Alchemy"
    }
}
