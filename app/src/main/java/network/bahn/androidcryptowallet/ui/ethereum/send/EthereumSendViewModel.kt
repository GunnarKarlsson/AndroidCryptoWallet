package network.bahn.androidcryptowallet.ui.ethereum.send

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
import network.bahn.androidcryptowallet.domain.model.EthereumFeeData
import network.bahn.androidcryptowallet.domain.model.EthereumGasPreset
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import network.bahn.androidcryptowallet.ui.navigation.EthereumSendRoute
import network.bahn.androidcryptowallet.ui.util.StringUtils
import java.math.BigInteger
import javax.inject.Inject

sealed interface EthereumSendEvent {
    data object Sent : EthereumSendEvent
}

@HiltViewModel
class EthereumSendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: EthereumWalletRepository,
) : ViewModel() {
    private val walletId: String =
        savedStateHandle.get<String>("walletId")
            ?: savedStateHandle.toRoute<EthereumSendRoute>().walletId
    private val wallet: StateFlow<EthereumWallet?> = walletRepository.observeWallet(walletId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
    private val form = MutableStateFlow(FormState())
    private val feeState = MutableStateFlow(FeeState(isLoading = true))
    private val eventsChannel = Channel<EthereumSendEvent>(Channel.BUFFERED)
    private var sendJob: Job? = null
    private var feeJob: Job? = null

    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<EthereumSendUiState> = combine(
        wallet,
        form,
        feeState,
    ) { currentWallet, formState, fees ->
        EthereumSendUiState(
            recipient = formState.recipient,
            amount = formState.amount,
            gasPreset = formState.gasPreset,
            isSubmitting = formState.isSubmitting,
            errorMessage = formState.errorMessage,
            availableBalanceWei = currentWallet?.balanceWei,
            feeData = fees.feeData,
            isLoadingFees = fees.isLoading,
            feeLoadError = fees.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EthereumSendUiState(isLoadingFees = true),
    )

    init {
        loadFees()
    }

    fun onRecipientChange(value: String) {
        form.update { it.copy(recipient = value, errorMessage = null) }
    }

    fun onAmountChange(value: String) {
        if (value.isEmpty() || value.matches(ETH_AMOUNT_PATTERN)) {
            form.update { it.copy(amount = value, errorMessage = null) }
        }
    }

    fun onGasPresetSelected(preset: EthereumGasPreset) {
        form.update { it.copy(gasPreset = preset) }
    }

    fun onRetryFees() {
        loadFees()
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
            if (feeState.value.feeData == null) {
                form.update { it.copy(errorMessage = FEES_FAILED) }
                return@launch
            }
            val amountWei = StringUtils.parseEthereumAmountToWei(current.amount)
            if (amountWei == null || amountWei <= BigInteger.ZERO) {
                form.update { it.copy(errorMessage = INVALID_AMOUNT) }
                return@launch
            }
            val recipient = current.recipient.trim()
            if (!walletRepository.isValidAddress(recipient)) {
                form.update { it.copy(errorMessage = INVALID_ADDRESS) }
                return@launch
            }
            form.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                walletRepository.send(
                    walletId = walletId,
                    recipientAddress = recipient,
                    amountWei = amountWei,
                    gasPreset = current.gasPreset,
                )
                eventsChannel.send(EthereumSendEvent.Sent)
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

    private fun loadFees() {
        if (feeJob?.isActive == true) return
        feeJob = viewModelScope.launch {
            feeState.value = FeeState(isLoading = true)
            try {
                val data = walletRepository.getFeeData(walletId)
                feeState.value = FeeState(feeData = data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fee load failed", e)
                feeState.value = FeeState(
                    errorMessage = e.message?.takeIf { it.isNotBlank() } ?: FEES_FAILED,
                )
            }
        }
    }

    private companion object {
        val ETH_AMOUNT_PATTERN = Regex("^\\d*\\.?\\d{0,18}$")
        const val TAG = "EthereumSend"
        const val INVALID_ADDRESS = "Enter a valid Ethereum address"
        const val INVALID_AMOUNT = "Enter an amount greater than zero"
        const val WALLET_MISSING = "Wallet not found"
        const val BROADCAST_FAILED = "Could not broadcast transaction"
        const val FEES_FAILED = "Could not load network fees"
    }
}

private data class FormState(
    val recipient: String = "",
    val amount: String = "",
    val gasPreset: EthereumGasPreset = EthereumGasPreset.Normal,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

private data class FeeState(
    val feeData: EthereumFeeData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
