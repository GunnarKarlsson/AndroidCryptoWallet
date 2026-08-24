package network.bahn.androidcryptowallet.ui.bitcoin.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

@HiltViewModel
class BitcoinSetupViewModel @Inject constructor(
    private val walletRepository: BitcoinWalletRepository,
    private val networkStatusRepository: BitcoinNetworkStatusRepository,
) : ViewModel() {
    private val session = BitcoinSetupSession(networkStatusRepository, viewModelScope)
    private val createNetwork = session.network
    private val mnemonicWords = MutableStateFlow<List<String>>(emptyList())
    private val passphrase = MutableStateFlow("")
    private val isCreating = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val eventsChannel = Channel<BitcoinSetupEvent>(Channel.BUFFERED)

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<BitcoinSetupUiState> = combine(
        createNetwork,
        mnemonicWords,
        passphrase,
        isCreating,
        errorMessage,
    ) { network, words, phrase, creating, error ->
        BitcoinSetupUiState(
            createNetwork = network,
            mnemonicWords = words,
            passphrase = phrase,
            isCreating = creating,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BitcoinSetupUiState(),
    )

    fun onCreateNetworkSelected(network: BitcoinNetwork) {
        createNetwork.value = network
    }

    fun onPassphraseChange(value: String) {
        passphrase.value = value
    }

    fun ensureMnemonicGenerated() {
        if (mnemonicWords.value.isEmpty()) {
            mnemonicWords.value = walletRepository.generateMnemonic()
        }
    }

    fun confirm() {
        ensureMnemonicGenerated()
        val words = mnemonicWords.value
        if (words.isEmpty()) return
        session.submit(
            submitting = isCreating,
            errorMessage = errorMessage,
            logTag = TAG,
            failureLog = "Create wallet failed",
            fallbackError = CREATE_FAILED_FALLBACK,
        ) {
            walletRepository.createWallet(
                network = createNetwork.value,
                mnemonicWords = words,
                passphrase = passphrase.value.takeIf { it.isNotBlank() },
            )
            networkStatusRepository.setNetwork(createNetwork.value)
            mnemonicWords.value = emptyList()
            passphrase.value = ""
            eventsChannel.send(BitcoinSetupEvent.WalletCreated)
        }
    }

    private companion object {
        const val TAG = "BitcoinSetup"
        const val CREATE_FAILED_FALLBACK = "Could not create wallet"
    }
}
