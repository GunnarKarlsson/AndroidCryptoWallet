package network.bahn.androidcryptowallet.ui.bitcoin.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

@HiltViewModel
class BitcoinRestoreViewModel @Inject constructor(
    private val walletRepository: BitcoinWalletRepository,
    private val networkStatusRepository: BitcoinNetworkStatusRepository,
) : ViewModel() {
    private val restoreNetwork = MutableStateFlow(BitcoinNetwork.TESTNET4)
    private val mnemonicWords = MutableStateFlow(List(RESTORE_MNEMONIC_WORD_COUNT) { "" })
    private val passphrase = MutableStateFlow("")
    private val isRestoring = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val eventsChannel = Channel<BitcoinRestoreEvent>(Channel.BUFFERED)

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<BitcoinRestoreUiState> = combine(
        restoreNetwork,
        mnemonicWords,
        passphrase,
        isRestoring,
        errorMessage,
    ) { network, words, phrase, restoring, error ->
        BitcoinRestoreUiState(
            restoreNetwork = network,
            mnemonicWords = words,
            passphrase = phrase,
            isRestoring = restoring,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BitcoinRestoreUiState(),
    )

    init {
        viewModelScope.launch {
            restoreNetwork.value = networkStatusRepository.selectedNetwork().first()
        }
    }

    fun onRestoreNetworkSelected(network: BitcoinNetwork) {
        restoreNetwork.value = network
    }

    fun onMnemonicWordChange(index: Int, value: String) {
        if (index !in 0 until RESTORE_MNEMONIC_WORD_COUNT) return
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
        if (words.any { it.isBlank() } || isRestoring.value) return
        viewModelScope.launch {
            isRestoring.value = true
            errorMessage.value = null
            try {
                walletRepository.restoreWallet(
                    network = restoreNetwork.value,
                    mnemonicWords = words,
                    passphrase = passphrase.value.takeIf { it.isNotBlank() },
                )
                networkStatusRepository.setNetwork(restoreNetwork.value)
                mnemonicWords.value = List(RESTORE_MNEMONIC_WORD_COUNT) { "" }
                passphrase.value = ""
                eventsChannel.send(BitcoinRestoreEvent.WalletRestored)
            } catch (e: Exception) {
                Log.e(TAG, "Restore wallet failed", e)
                errorMessage.value = e.message?.takeIf { it.isNotBlank() }
                    ?: RESTORE_FAILED_FALLBACK
            } finally {
                isRestoring.value = false
            }
        }
    }

    private companion object {
        const val TAG = "BitcoinRestore"
        const val RESTORE_FAILED_FALLBACK = "Could not restore wallet"
    }
}
