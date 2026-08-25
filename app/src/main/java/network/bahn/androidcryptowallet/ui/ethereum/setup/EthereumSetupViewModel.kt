package network.bahn.androidcryptowallet.ui.ethereum.setup

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
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkStore
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import javax.inject.Inject

@HiltViewModel
class EthereumSetupViewModel @Inject constructor(
    private val walletRepository: EthereumWalletRepository,
    private val selectedEthereumNetworkStore: SelectedEthereumNetworkStore,
) : ViewModel() {
    private val session = EthereumSetupSession(selectedEthereumNetworkStore, viewModelScope)
    private val createNetwork = session.network
    private val mnemonicWords = MutableStateFlow<List<String>>(emptyList())
    private val passphrase = MutableStateFlow("")
    private val isCreating = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val eventsChannel = Channel<EthereumSetupEvent>(Channel.BUFFERED)

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<EthereumSetupUiState> = combine(
        createNetwork,
        mnemonicWords,
        passphrase,
        isCreating,
        errorMessage,
    ) { network, words, phrase, creating, error ->
        EthereumSetupUiState(
            createNetwork = network,
            mnemonicWords = words,
            passphrase = phrase,
            isCreating = creating,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EthereumSetupUiState(),
    )

    fun onCreateNetworkSelected(network: EthereumNetwork) {
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
            selectedEthereumNetworkStore.setNetwork(createNetwork.value)
            mnemonicWords.value = emptyList()
            passphrase.value = ""
            eventsChannel.send(EthereumSetupEvent.WalletCreated)
        }
    }

    private companion object {
        const val TAG = "EthereumSetup"
        const val CREATE_FAILED_FALLBACK = "Could not create wallet"
    }
}
