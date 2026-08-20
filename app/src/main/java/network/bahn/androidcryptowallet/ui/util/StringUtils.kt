package network.bahn.androidcryptowallet.ui.util

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

object StringUtils {
    private const val BLOCK_HEIGHT_PLACEHOLDER = "—"

    fun formatBlockHeight(height: Long?): String {
        if (height == null) return BLOCK_HEIGHT_PLACEHOLDER
        return NumberFormat.getIntegerInstance(Locale.US).format(height)
    }

    fun formatLastUpdated(
        updatedAtMillis: Long?,
        neverRefreshed: String,
        lastUpdatedPattern: String,
    ): String {
        if (updatedAtMillis == null) return neverRefreshed
        return lastUpdatedPattern.format(formatDateTime(updatedAtMillis))
    }

    fun formatDateTime(epochMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(epochMillis))

    fun truncateBitcoinAddress(address: String, head: Int = 8, tail: Int = 8): String {
        if (address.length <= head + tail + 1) return address
        return address.take(head) + "…" + address.takeLast(tail)
    }

    fun formatBitcoinAmount(satoshis: Long?): String {
        if (satoshis == null) return BLOCK_HEIGHT_PLACEHOLDER
        val btc = satoshis.toBigDecimal().movePointLeft(8)
        return BTC_AMOUNT_FORMAT.format(btc)
    }

    private val BTC_AMOUNT_FORMAT = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 8
        maximumFractionDigits = 8
        isGroupingUsed = false
    }
}
