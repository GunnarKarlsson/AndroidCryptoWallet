package network.bahn.androidcryptowallet.ui.ethereum.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEvmNetworkStore
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository
import network.bahn.androidcryptowallet.ui.navigation.EvmRestoreGraphRoute
import javax.inject.Inject

@HiltViewModel
class EvmRestoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: EvmWalletRepository,
    private val selectedEvmNetworkStore: SelectedEvmNetworkStore,
) : ViewModel() {
    private val family: EvmFamily = savedStateHandle.toRoute<EvmRestoreGraphRoute>().family
    private val availableNetworks = EvmNetwork.networksFor(family)
    private val session = EvmSetupSession(selectedEvmNetworkStore, family, viewModelScope)
    private val restoreNetwork = session.network
    private val mnemonicWords = MutableStateFlow(List(ETH_RESTORE_MNEMONIC_WORD_COUNT) { "" })
    private val passphrase = MutableStateFlow("")
    private val isRestoring = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val eventsChannel = Channel<EvmRestoreEvent>(Channel.BUFFERED)

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<EvmRestoreUiState> = combine(
        restoreNetwork,
        mnemonicWords,
        passphrase,
        isRestoring,
        errorMessage,
    ) { network, words, phrase, restoring, error ->
        EvmRestoreUiState(
            family = family,
            availableNetworks = availableNetworks,
            restoreNetwork = network,
            mnemonicWords = words,
            passphrase = phrase,
            isRestoring = restoring,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EvmRestoreUiState(
            family = family,
            availableNetworks = availableNetworks,
        ),
    )

    fun onRestoreNetworkSelected(network: EvmNetwork) {
        if (network.family != family) return
        restoreNetwork.value = network
    }

    fun onMnemonicWordChange(index: Int, value: String) {
        if (index !in 0 until ETH_RESTORE_MNEMONIC_WORD_COUNT) return
        val words = mnemonicWords.value.toMutableList()
        val normalized = value.lowercase()
        val parts = normalized.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        when {
            parts.isEmpty() -> words[index] = ""
            parts.size == 1 && !normalized.last().isWhitespace() -> words[index] = parts[0]
            else -> parts.forEachIndexed { offset, word ->
                val target = index + offset
                if (target in words.indices) words[target] = word
            }
        }
        mnemonicWords.value = words
    }

    fun onPassphraseChange(value: String) {
        passphrase.value = value
    }

    fun restore() {
        val words = mnemonicWords.value.map { it.trim() }
        if (words.any { it.isBlank() }) return
        session.submit(
            submitting = isRestoring,
            errorMessage = errorMessage,
            logTag = TAG,
            failureLog = "Restore wallet failed",
            fallbackError = RESTORE_FAILED_FALLBACK,
        ) {
            walletRepository.restoreWallet(
                network = restoreNetwork.value,
                mnemonicWords = words,
                passphrase = passphrase.value.takeIf { it.isNotBlank() },
            )
            selectedEvmNetworkStore.setNetwork(family, restoreNetwork.value)
            mnemonicWords.value = List(ETH_RESTORE_MNEMONIC_WORD_COUNT) { "" }
            passphrase.value = ""
            eventsChannel.send(EvmRestoreEvent.WalletRestored)
        }
    }

    private companion object {
        const val TAG = "EvmRestore"
        const val RESTORE_FAILED_FALLBACK = "Could not restore wallet"
    }
}
