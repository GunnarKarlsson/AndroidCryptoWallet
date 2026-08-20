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
}
