package network.bahn.androidcryptowallet.ui.bitcoin.edit

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
import network.bahn.androidcryptowallet.domain.usecase.ObserveBitcoinWalletUseCase
import network.bahn.androidcryptowallet.domain.usecase.RenameBitcoinWalletUseCase
import network.bahn.androidcryptowallet.ui.navigation.BitcoinEditWalletRoute
import javax.inject.Inject

sealed interface BitcoinEditWalletEvent {
    data object Saved : BitcoinEditWalletEvent
}

@HiltViewModel
class BitcoinEditWalletViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBitcoinWallet: ObserveBitcoinWalletUseCase,
    private val renameBitcoinWallet: RenameBitcoinWalletUseCase,
) : ViewModel() {
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<BitcoinEditWalletRoute>().walletId
    private val form = MutableStateFlow(FormState())
    private val eventsChannel = Channel<BitcoinEditWalletEvent>(Channel.BUFFERED)
    private var confirmJob: Job? = null

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<BitcoinEditWalletUiState> = combine(
        observeBitcoinWallet(walletId),
        form,
    ) { wallet, formState ->
        BitcoinEditWalletUiState(
            name = formState.name,
            isSubmitting = formState.isSubmitting,
            errorMessage = formState.errorMessage,
            isWalletLoaded = wallet != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BitcoinEditWalletUiState(),
    )

    init {
        viewModelScope.launch {
            val wallet = observeBitcoinWallet(walletId).filterNotNull().first()
            if (!form.value.userEdited) {
                form.update {
                    it.copy(
                        name = wallet.name?.trim()?.takeIf { name -> name.isNotEmpty() }
                            ?: DEFAULT_NAME,
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
                renameBitcoinWallet(walletId, form.value.name)
                eventsChannel.send(BitcoinEditWalletEvent.Saved)
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
        const val TAG = "BitcoinEditWallet"
        const val DEFAULT_NAME = "Bitcoin wallet"
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
