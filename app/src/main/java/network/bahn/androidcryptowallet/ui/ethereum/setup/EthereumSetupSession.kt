package network.bahn.androidcryptowallet.ui.ethereum.setup

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkStore
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork

internal class EthereumSetupSession(
    private val selectedEthereumNetworkStore: SelectedEthereumNetworkStore,
    private val scope: CoroutineScope,
) {
    val network = MutableStateFlow(EthereumNetwork.SEPOLIA)

    init {
        scope.launch {
            network.value = selectedEthereumNetworkStore.selectedNetwork.first()
        }
    }

    fun submit(
        submitting: MutableStateFlow<Boolean>,
        errorMessage: MutableStateFlow<String?>,
        logTag: String,
        failureLog: String,
        fallbackError: String,
        block: suspend () -> Unit,
    ) {
        if (submitting.value) return
        scope.launch {
            submitting.value = true
            errorMessage.value = null
            try {
                block()
            } catch (e: Exception) {
                Log.e(logTag, failureLog, e)
                errorMessage.value = e.message?.takeIf { it.isNotBlank() } ?: fallbackError
            } finally {
                submitting.value = false
            }
        }
    }
}
