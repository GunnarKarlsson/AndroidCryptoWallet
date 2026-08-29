package network.bahn.androidcryptowallet.ui.ethereum.edit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository
import network.bahn.androidcryptowallet.ui.chain.EvmFamilyDefaultNames
import network.bahn.androidcryptowallet.ui.navigation.EvmEditWalletRoute
import javax.inject.Inject

sealed interface EthereumEditWalletEvent {
    data object Saved : EthereumEditWalletEvent
}

@HiltViewModel
class EthereumEditWalletViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: EvmWalletRepository,
    private val defaultNames: EvmFamilyDefaultNames,
) : ViewModel() {
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<EvmEditWalletRoute>().walletId
    private val form = MutableStateFlow(FormState())
    private val eventsChannel = Channel<EthereumEditWalletEvent>(Channel.BUFFERED)
    private var confirmJob: Job? = null

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<EthereumEditWalletUiState> = combine(
        walletRepository.observeWallet(walletId),
        form,
    ) { wallet, formState ->
        EthereumEditWalletUiState(
            family = wallet?.network?.family,
            name = formState.name,
            isSubmitting = formState.isSubmitting,
            errorMessage = formState.errorMessage,
            isWalletLoaded = wallet != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EthereumEditWalletUiState(),
    )

    init {
        viewModelScope.launch {
            val wallet = walletRepository.observeWallet(walletId).filterNotNull().first()
            if (!form.value.userEdited) {
                val defaultName = defaultNames.walletListName(wallet.network.family)
                form.update {
                    it.copy(
                        name = wallet.name?.trim()?.takeIf { name -> name.isNotEmpty() }
                            ?: defaultName,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        form.update {
            it.copy(
                name = value.take(MAX_NAME_LENGTH),
                errorMessage = null,
                userEdited = true,
            )
        }
    }

    fun onConfirm() {
        if (confirmJob?.isActive == true) return
        confirmJob = viewModelScope.launch {
            form.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                walletRepository.renameWallet(walletId, form.value.name)
                eventsChannel.send(EthereumEditWalletEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Rename failed", e)
                form.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message?.takeIf { message -> message.isNotBlank() }
                            ?: SAVE_FAILED,
                    )
                }
                return@launch
            }
            form.update { it.copy(isSubmitting = false) }
        }
    }

    private companion object {
        const val TAG = "EthereumEditWallet"
        const val MAX_NAME_LENGTH = 40
        const val SAVE_FAILED = "Could not save wallet name"
    }
}

private data class FormState(
    val name: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val userEdited: Boolean = false,
)
