package network.bahn.androidcryptowallet.ui.bitcoin.send

import androidx.annotation.StringRes
import network.bahn.androidcryptowallet.R

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

data class BitcoinSendUiState(
    val recipient: String = "",
    val amount: String = "",
    val feePreset: SendFeePreset = SendFeePreset.Normal,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isWatchOnly: Boolean = false,
) {
    val canSend: Boolean
        get() = recipient.isNotBlank() &&
            amount.isNotBlank() &&
            !isWatchOnly &&
            !isSubmitting
}
