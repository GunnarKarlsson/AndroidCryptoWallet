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

    fun truncateEthereumAddress(address: String, head: Int = 8, tail: Int = 8): String =
        truncateBitcoinAddress(address, head, tail)

    /** 1 BTC = 100_000_000 satoshis. Always eight fractional digits, including zero. */
    fun formatBitcoinAmount(satoshis: Long): String =
        satoshis.toBigDecimal()
            .movePointLeft(8)
            .setScale(8, java.math.RoundingMode.UNNECESSARY)
            .toPlainString()

    fun walletDisplayName(name: String?, fallback: String): String =
        name?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

    /** 1 ETH = 10^18 wei. Always eighteen fractional digits, including zero. */
    fun formatEthereumAmount(wei: String): String =
        wei.toBigDecimal()
            .movePointLeft(18)
            .setScale(18, java.math.RoundingMode.UNNECESSARY)
            .toPlainString()

    fun parseBitcoinAmountToSatoshis(amount: String): Long? {
        val trimmed = amount.trim()
        if (trimmed.isEmpty() || trimmed == ".") return null
        return runCatching {
            trimmed.toBigDecimal()
                .movePointRight(8)
                .setScale(0, java.math.RoundingMode.DOWN)
                .longValueExact()
        }.getOrNull()
    }

    /** 1 ETH = 10^18 wei. Truncates toward zero past 18 fractional digits. */
    fun parseEthereumAmountToWei(amount: String): java.math.BigInteger? {
        val trimmed = amount.trim()
        if (trimmed.isEmpty() || trimmed == ".") return null
        return runCatching {
            trimmed.toBigDecimal()
                .movePointRight(18)
                .setScale(0, java.math.RoundingMode.DOWN)
                .toBigIntegerExact()
        }.getOrNull()
    }
}
