package network.bahn.androidcryptowallet.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.usecase.RefreshBitcoinBlockHeightUseCase
import network.bahn.androidcryptowallet.domain.usecase.SetBitcoinNetworkUseCase
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val setBitcoinNetwork: SetBitcoinNetworkUseCase,
    private val refreshBlockHeight: RefreshBitcoinBlockHeightUseCase,
) : ViewModel() {
    fun onNetworkSelected(network: BitcoinNetwork) {
        viewModelScope.launch {
            setBitcoinNetwork(network)
        }
    }

    fun onRefresh(network: BitcoinNetwork) {
        viewModelScope.launch {
            try {
                setBitcoinNetwork(network)
                refreshBlockHeight()
            } catch (e: Exception) {
                Log.e(TAG, "Refresh failed for $network", e)
            }
        }
    }

    private companion object {
        const val TAG = "Alchemy"
    }
}
