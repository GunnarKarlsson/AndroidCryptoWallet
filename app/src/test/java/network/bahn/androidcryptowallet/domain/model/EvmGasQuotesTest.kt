package network.bahn.androidcryptowallet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EvmGasQuotesTest {
    @Test
    fun quoteAppliesPriorityMultiplierAndBaseFeeHeadroom() {
        val feeData = EvmFeeData(
            baseFeePerGasWei = "1000000000",
            suggestedPriorityFeePerGasWei = "2000000000",
        )

        val quote = EvmGasQuotes.quote(feeData, EvmGasPreset.Fast)

        // priority = 2e9 * 1.5 = 3e9; maxFee = 2*1e9 + 3e9 = 5e9
        assertEquals(21_000L, quote.gasLimit)
        assertEquals("3000000000", quote.maxPriorityFeePerGasWei)
        assertEquals("5000000000", quote.maxFeePerGasWei)
        assertEquals((21_000L * 5_000_000_000L).toString(), quote.estimatedFeeWei)
    }

    @Test
    fun quoteFloorsPriorityAtOneWei() {
        val feeData = EvmFeeData(
            baseFeePerGasWei = "0",
            suggestedPriorityFeePerGasWei = "0",
        )

        val quote = EvmGasQuotes.quote(feeData, EvmGasPreset.Slow)

        assertEquals("1", quote.maxPriorityFeePerGasWei)
        assertEquals("1", quote.maxFeePerGasWei)
    }
}
