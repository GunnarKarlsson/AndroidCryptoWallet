package network.bahn.androidcryptowallet.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsTest {
    @Test
    fun formatBlockHeightUsesPlaceholderWhenNull() {
        assertEquals("—", StringUtils.formatBlockHeight(null))
    }

    @Test
    fun formatBlockHeightGroupsThousands() {
        assertEquals("894,623", StringUtils.formatBlockHeight(894_623))
    }

    @Test
    fun formatLastUpdatedUsesNeverRefreshedWhenNull() {
        assertEquals(
            "Not yet refreshed",
            StringUtils.formatLastUpdated(
                updatedAtMillis = null,
                neverRefreshed = "Not yet refreshed",
                lastUpdatedPattern = "Last updated %1\$s",
            ),
        )
    }

    @Test
    fun formatLastUpdatedInsertsFormattedTime() {
        val result = StringUtils.formatLastUpdated(
            updatedAtMillis = 1_700_000_000_000L,
            neverRefreshed = "Not yet refreshed",
            lastUpdatedPattern = "Last updated %1\$s",
        )
        assertEquals("Last updated ${StringUtils.formatDateTime(1_700_000_000_000L)}", result)
    }

    @Test
    fun truncateBitcoinAddressKeepsHeadAndTail() {
        assertEquals(
            "bc1qcr8t…8z306fyu",
            StringUtils.truncateBitcoinAddress("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"),
        )
    }

    @Test
    fun truncateBitcoinAddressLeavesShortAddressesAlone() {
        assertEquals("bc1qshort", StringUtils.truncateBitcoinAddress("bc1qshort"))
    }

    @Test
    fun formatBitcoinAmountAlwaysShowsEightFractionalDigits() {
        assertEquals("0.00000000", StringUtils.formatBitcoinAmount(0))
        assertEquals("0.00000001", StringUtils.formatBitcoinAmount(1))
        assertEquals("0.04225100", StringUtils.formatBitcoinAmount(4_225_100))
        assertEquals("1.00000000", StringUtils.formatBitcoinAmount(100_000_000))
    }

    @Test
    fun parseBitcoinAmountToSatoshisReadsBtcString() {
        assertEquals(null, StringUtils.parseBitcoinAmountToSatoshis(""))
        assertEquals(null, StringUtils.parseBitcoinAmountToSatoshis("."))
        assertEquals(0L, StringUtils.parseBitcoinAmountToSatoshis("0"))
        assertEquals(1L, StringUtils.parseBitcoinAmountToSatoshis("0.00000001"))
        assertEquals(1_000_000L, StringUtils.parseBitcoinAmountToSatoshis("0.01"))
        assertEquals(100_000_000L, StringUtils.parseBitcoinAmountToSatoshis("1"))
    }
}
