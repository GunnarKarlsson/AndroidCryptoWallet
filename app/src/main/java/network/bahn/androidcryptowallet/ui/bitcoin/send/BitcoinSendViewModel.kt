package network.bahn.androidcryptowallet.ui.bitcoin.send

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.ui.navigation.BitcoinSendRoute
import network.bahn.androidcryptowallet.ui.util.StringUtils
import javax.inject.Inject

sealed interface BitcoinSendEvent {
    data object Sent : BitcoinSendEvent
}

@HiltViewModel
class BitcoinSendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: BitcoinWalletRepository,
) : ViewModel() {
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<BitcoinSendRoute>().walletId
    private val wallet: StateFlow<BitcoinWallet?> = walletRepository.observeWallet(walletId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
    private val form = MutableStateFlow(FormState())
    private val eventsChannel = Channel<BitcoinSendEvent>(Channel.BUFFERED)
    private var sendJob: Job? = null

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<BitcoinSendUiState> = combine(
        wallet,
        form,
    ) { currentWallet, formState ->
        BitcoinSendUiState(
            recipient = formState.recipient,
            amount = formState.amount,
            feePreset = formState.feePreset,
            isSubmitting = formState.isSubmitting,
            errorMessage = formState.errorMessage,
            isWatchOnly = currentWallet?.kind == BitcoinWalletKind.WATCH_ONLY,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BitcoinSendUiState(),
    )

    fun onRecipientChange(value: String) {
        form.update { it.copy(recipient = value, errorMessage = null) }
    }

    fun onAmountChange(value: String) {
        if (value.isEmpty() || value.matches(BTC_AMOUNT_PATTERN)) {
            form.update { it.copy(amount = value, errorMessage = null) }
        }
    }

    fun onFeePresetSelected(preset: SendFeePreset) {
        form.update { it.copy(feePreset = preset) }
    }

    fun onSend() {
        if (sendJob?.isActive == true) return
        val current = form.value
        sendJob = viewModelScope.launch {
            val currentWallet = walletRepository.observeWallet(walletId).first()
            if (currentWallet == null) {
                form.update { it.copy(errorMessage = WALLET_MISSING) }
                return@launch
            }
            if (currentWallet.kind == BitcoinWalletKind.WATCH_ONLY) {
                form.update { it.copy(errorMessage = WATCH_ONLY) }
                return@launch
            }
            val amountSatoshis = StringUtils.parseBitcoinAmountToSatoshis(current.amount)
            if (amountSatoshis == null || amountSatoshis <= 0L) {
                form.update { it.copy(errorMessage = INVALID_AMOUNT) }
                return@launch
            }
            if (amountSatoshis < MIN_P2WPKH_OUTPUT_SATOSHIS) {
                form.update { it.copy(errorMessage = BELOW_DUST) }
                return@launch
            }
            val recipient = current.recipient.trim()
            if (!walletRepository.isValidAddress(currentWallet.network, recipient)) {
                form.update { it.copy(errorMessage = INVALID_ADDRESS) }
                return@launch
            }
            form.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                walletRepository.send(
                    walletId = walletId,
                    recipientAddress = recipient,
                    amountSatoshis = amountSatoshis,
                    feeRateSatPerVbyte = current.feePreset.satPerVByte,
                )
                eventsChannel.send(BitcoinSendEvent.Sent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Send failed", e)
                form.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message?.takeIf { message -> message.isNotBlank() }
                            ?: BROADCAST_FAILED,
                    )
                }
                return@launch
            }
            form.update { it.copy(isSubmitting = false) }
        }
    }

    private companion object {
        val BTC_AMOUNT_PATTERN = Regex("^\\d*\\.?\\d{0,8}$")
        const val TAG = "BitcoinSend"
        // Bitcoin Core P2WPKH dust: 3 sat/vB × (31-byte output + 67 vB to spend) = 294.
        const val MIN_P2WPKH_OUTPUT_SATOSHIS = 294L
        const val INVALID_ADDRESS = "Enter a valid Bitcoin address for this network"
        const val INVALID_AMOUNT = "Enter an amount greater than zero"
        const val BELOW_DUST =
            "Amount is below the dust limit. Send at least 294 satoshis (0.00000294 BTC)."
        const val WATCH_ONLY = "Watch-only wallets cannot send"
        const val WALLET_MISSING = "Wallet not found"
        const val BROADCAST_FAILED = "Could not broadcast transaction"
    }
}

private data class FormState(
    val recipient: String = "",
    val amount: String = "",
    val feePreset: SendFeePreset = SendFeePreset.Normal,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)
