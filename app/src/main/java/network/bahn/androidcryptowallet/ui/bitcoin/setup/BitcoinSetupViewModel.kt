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
import network.bahn.androidcryptowallet.domain.usecase.CreateBitcoinWalletUseCase
import network.bahn.androidcryptowallet.domain.usecase.GenerateBitcoinMnemonicUseCase
import network.bahn.androidcryptowallet.domain.usecase.ObserveSelectedBitcoinNetworkUseCase
import javax.inject.Inject

@HiltViewModel
class BitcoinSetupViewModel @Inject constructor(
    observeSelectedBitcoinNetwork: ObserveSelectedBitcoinNetworkUseCase,
    private val generateBitcoinMnemonic: GenerateBitcoinMnemonicUseCase,
    private val createBitcoinWallet: CreateBitcoinWalletUseCase,
) : ViewModel() {
    private val createNetwork = MutableStateFlow(BitcoinNetwork.TESTNET4)
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

    init {
        viewModelScope.launch {
            createNetwork.value = observeSelectedBitcoinNetwork().first()
        }
    }

    fun onCreateNetworkSelected(network: BitcoinNetwork) {
        createNetwork.value = network
    }

    fun onPassphraseChange(value: String) {
        passphrase.value = value
    }

    fun ensureMnemonicGenerated() {
        if (mnemonicWords.value.isEmpty()) {
            mnemonicWords.value = generateBitcoinMnemonic()
        }
    }

    fun confirm() {
        ensureMnemonicGenerated()
        val words = mnemonicWords.value
        if (words.isEmpty() || isCreating.value) return
        viewModelScope.launch {
            isCreating.value = true
            errorMessage.value = null
            try {
                createBitcoinWallet(
                    network = createNetwork.value,
                    mnemonicWords = words,
                    passphrase = passphrase.value.takeIf { it.isNotBlank() },
                )
                mnemonicWords.value = emptyList()
                passphrase.value = ""
                eventsChannel.send(BitcoinSetupEvent.WalletCreated)
            } catch (e: Exception) {
                Log.e(TAG, "Create wallet failed", e)
                errorMessage.value = e.message?.takeIf { it.isNotBlank() }
                    ?: CREATE_FAILED_FALLBACK
            } finally {
                isCreating.value = false
            }
        }
    }

    private companion object {
        const val TAG = "BitcoinSetup"
        const val CREATE_FAILED_FALLBACK = "Could not create wallet"
    }
}
