package network.bahn.androidcryptowallet.ui.evm.setup

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEvmNetworkStore
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork

internal class EvmSetupSession(
    private val selectedEvmNetworkStore: SelectedEvmNetworkStore,
    private val family: EvmFamily,
    private val scope: CoroutineScope,
) {
    val network = MutableStateFlow(defaultNetwork())

    init {
        scope.launch {
            network.value = selectedEvmNetworkStore.selectedNetwork(family).first()
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

    private fun defaultNetwork(): EvmNetwork =
        EvmNetwork.networksFor(family).firstOrNull() ?: EvmNetwork.SEPOLIA
}
