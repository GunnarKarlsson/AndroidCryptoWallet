package network.bahn.androidcryptowallet.ui.ethereum.send

import androidx.annotation.StringRes
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EthereumFeeData
import network.bahn.androidcryptowallet.domain.model.EthereumGasPreset
import network.bahn.androidcryptowallet.domain.model.EthereumGasQuotes
import network.bahn.androidcryptowallet.ui.util.StringUtils
import java.math.BigInteger
import java.math.RoundingMode

data class EthereumSendUiState(
    val recipient: String = "",
    val amount: String = "",
    val gasPreset: EthereumGasPreset = EthereumGasPreset.Normal,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val availableBalanceWei: String? = null,
    val feeData: EthereumFeeData? = null,
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
            return EthereumGasQuotes.quote(data, gasPreset).estimatedFeeWei
        }

    /** Balance minus send amount (if any) and the selected fee. */
    val remainingBalanceWei: String?
        get() {
            val available = availableBalanceWei?.let { BigInteger(it) } ?: return null
            val amountWei = StringUtils.parseEthereumAmountToWei(amount) ?: BigInteger.ZERO
            val feeWei = estimatedFeeWei?.let { BigInteger(it) } ?: BigInteger.ZERO
            return available.subtract(amountWei).subtract(feeWei).toString()
        }

    val wouldOverspend: Boolean
        get() {
            val remaining = remainingBalanceWei?.let { BigInteger(it) } ?: return false
            return remaining < BigInteger.ZERO
        }

    fun priorityFeeGweiLabel(preset: EthereumGasPreset): String? {
        val data = feeData ?: return null
        val priorityWei = BigInteger(
            EthereumGasQuotes.quote(data, preset).maxPriorityFeePerGasWei,
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
fun EthereumGasPreset.labelRes(): Int = when (this) {
    EthereumGasPreset.Slow -> R.string.send_fee_slow
    EthereumGasPreset.Normal -> R.string.send_fee_normal
    EthereumGasPreset.Fast -> R.string.send_fee_fast
}

@StringRes
fun EthereumGasPreset.etaRes(): Int = when (this) {
    EthereumGasPreset.Slow -> R.string.eth_send_fee_slow_eta
    EthereumGasPreset.Normal -> R.string.eth_send_fee_normal_eta
    EthereumGasPreset.Fast -> R.string.eth_send_fee_fast_eta
}
