package network.bahn.androidcryptowallet.ui.bitcoin.setup

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository

internal class BitcoinSetupSession(
    private val networkStatusRepository: BitcoinNetworkStatusRepository,
    private val scope: CoroutineScope,
) {
    val network = MutableStateFlow(BitcoinNetwork.TESTNET4)

    init {
        scope.launch {
            network.value = networkStatusRepository.selectedNetwork().first()
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
