package network.bahn.androidcryptowallet.ui.bitcoin.send

import androidx.annotation.StringRes
import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.ui.util.StringUtils

enum class SendFeePreset(
    @StringRes val labelRes: Int,
    @StringRes val rateRes: Int,
    @StringRes val etaRes: Int,
    val satPerVByte: Long,
) {
    Slow(R.string.send_fee_slow, R.string.send_fee_slow_rate, R.string.send_fee_slow_eta, 2),
    Normal(R.string.send_fee_normal, R.string.send_fee_normal_rate, R.string.send_fee_normal_eta, 5),
    Fast(R.string.send_fee_fast, R.string.send_fee_fast_rate, R.string.send_fee_fast_eta, 10),
}

/** Typical single-input P2WPKH send size used for the fee estimate on the send screen. */
internal const val ESTIMATED_TX_VBYTES = 141L

data class BitcoinSendUiState(
    val recipient: String = "",
    val amount: String = "",
    val feePreset: SendFeePreset = SendFeePreset.Normal,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isWatchOnly: Boolean = false,
    val availableBalanceSatoshis: Long? = null,
) {
    val canSend: Boolean
        get() = recipient.isNotBlank() &&
            amount.isNotBlank() &&
            !isWatchOnly &&
            !isSubmitting

    val estimatedFeeSatoshis: Long
        get() = feePreset.satPerVByte * ESTIMATED_TX_VBYTES

    /** Confirmed balance minus send amount (if any) and the selected fee. */
    val remainingBalanceSatoshis: Long?
        get() {
            val available = availableBalanceSatoshis ?: return null
            val amountSatoshis = StringUtils.parseBitcoinAmountToSatoshis(amount) ?: 0L
            return available - amountSatoshis - estimatedFeeSatoshis
        }

    val wouldOverspend: Boolean
        get() {
            val remaining = remainingBalanceSatoshis ?: return false
            return remaining < 0L
        }
}
