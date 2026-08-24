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
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import javax.inject.Inject

@HiltViewModel
class BitcoinNetworkStatusViewModel @Inject constructor(
    private val networkStatusRepository: BitcoinNetworkStatusRepository,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BitcoinNetworkStatusUiState> = combine(
        networkStatusRepository.selectedNetwork(),
        networkStatusRepository.observeStatus(),
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
            onRefresh(networkStatusRepository.selectedNetwork().first())
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
                networkStatusRepository.setNetwork(network)
                networkStatusRepository.refreshBlockHeight()
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
