package network.bahn.androidcryptowallet.ui.evm.send

import androidx.annotation.StringRes
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmFeeData
import network.bahn.androidcryptowallet.domain.model.EvmGasPreset
import network.bahn.androidcryptowallet.domain.model.EvmGasQuotes
import network.bahn.androidcryptowallet.ui.util.StringUtils
import java.math.BigInteger
import java.math.RoundingMode

data class EvmSendUiState(
    val nativeSymbol: String = EvmNetwork.SEPOLIA.nativeSymbol,
    val recipient: String = "",
    val amount: String = "",
    val gasPreset: EvmGasPreset = EvmGasPreset.Normal,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val availableBalanceWei: String? = null,
    val feeData: EvmFeeData? = null,
    val isLoadingFees: Boolean = false,
    val feeLoadError: String? = null,
) {
    val canSend: Boolean
        get() = recipient.isNotBlank() &&
            amount.isNotBlank() &&
            feeData != null &&
            !isLoadingFees &&
            !isSubmitting

    val estimatedFeeWei: String?
        get() {
            val data = feeData ?: return null
            return EvmGasQuotes.quote(data, gasPreset).estimatedFeeWei
        }

    /** Balance minus send amount (if any) and the selected fee. */
    val remainingBalanceWei: String?
        get() {
            val available = availableBalanceWei?.let { BigInteger(it) } ?: return null
            val amountWei = StringUtils.parseEvmAmountToWei(amount) ?: BigInteger.ZERO
            val feeWei = estimatedFeeWei?.let { BigInteger(it) } ?: BigInteger.ZERO
            return available.subtract(amountWei).subtract(feeWei).toString()
        }

    val wouldOverspend: Boolean
        get() {
            val remaining = remainingBalanceWei?.let { BigInteger(it) } ?: return false
            return remaining < BigInteger.ZERO
        }

    fun priorityFeeGweiLabel(preset: EvmGasPreset): String? {
        val data = feeData ?: return null
        val priorityWei = BigInteger(
            EvmGasQuotes.quote(data, preset).maxPriorityFeePerGasWei,
        )
        val gwei = priorityWei.toBigDecimal()
            .movePointLeft(9)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        return gwei
    }
}

@StringRes
fun EvmGasPreset.labelRes(): Int = when (this) {
    EvmGasPreset.Slow -> R.string.send_fee_slow
    EvmGasPreset.Normal -> R.string.send_fee_normal
    EvmGasPreset.Fast -> R.string.send_fee_fast
}

@StringRes
fun EvmGasPreset.etaRes(): Int = when (this) {
    EvmGasPreset.Slow -> R.string.eth_send_fee_slow_eta
    EvmGasPreset.Normal -> R.string.eth_send_fee_normal_eta
    EvmGasPreset.Fast -> R.string.eth_send_fee_fast_eta
}
