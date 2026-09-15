package network.bahn.androidcryptowallet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger

class HoldingsValueCalculatorTest {
    @Test
    fun bitcoinUsesSatoshisTimesUsdPrice() {
        val holding = PortfolioHolding(
            destination = PortfolioHoldingDestination.Bitcoin,
            headline = "Bitcoin (BTC)",
            nativeSymbol = "BTC",
            balanceSatoshis = 50_000_000L,
        )
        val prices = mapOf(NativeAssetId.BITCOIN to price(NativeAssetId.BITCOIN, 100_000_000_000L))

        assertEquals(50_000_000_000L, HoldingsValueCalculator.holdingUsdMicros(holding, prices))
    }

    @Test
    fun bitcoinRoundsHalfUpToMicros() {
        assertEquals(
            1L,
            HoldingsValueCalculator.satoshisToUsdMicros(satoshis = 50L, priceUsdMicros = 1_000_000L),
        )
        assertEquals(
            0L,
            HoldingsValueCalculator.satoshisToUsdMicros(satoshis = 49L, priceUsdMicros = 1_000_000L),
        )
    }

    @Test
    fun evmUsesWeiTimesUsdPrice() {
        val holding = PortfolioHolding(
            destination = PortfolioHoldingDestination.Evm(EvmFamily.ETHEREUM),
            headline = "Ethereum (ETH)",
            nativeSymbol = "ETH",
            balanceWei = BigInteger("1000000000000000000"),
        )
        val prices = mapOf(NativeAssetId.ETHEREUM to price(NativeAssetId.ETHEREUM, 3_500_000_000L))

        assertEquals(3_500_000_000L, HoldingsValueCalculator.holdingUsdMicros(holding, prices))
    }

    @Test
    fun l2UsesEthereumPrice() {
        val holding = PortfolioHolding(
            destination = PortfolioHoldingDestination.Evm(EvmFamily.ARBITRUM),
            headline = "Arbitrum (ETH)",
            nativeSymbol = "ETH",
            balanceWei = BigInteger("2000000000000000000"),
        )
        val prices = mapOf(NativeAssetId.ETHEREUM to price(NativeAssetId.ETHEREUM, 1_000_000_000L))

        assertEquals(2_000_000_000L, HoldingsValueCalculator.holdingUsdMicros(holding, prices))
    }

    @Test
    fun missingPriceReturnsNullForHolding() {
        val holding = PortfolioHolding(
            destination = PortfolioHoldingDestination.Bitcoin,
            headline = "Bitcoin (BTC)",
            nativeSymbol = "BTC",
            balanceSatoshis = 100L,
        )
        assertNull(HoldingsValueCalculator.holdingUsdMicros(holding, emptyMap()))
    }

    @Test
    fun zeroBalanceWithPriceIsZeroMicros() {
        val holding = PortfolioHolding(
            destination = PortfolioHoldingDestination.Bitcoin,
            headline = "Bitcoin (BTC)",
            nativeSymbol = "BTC",
            balanceSatoshis = 0L,
        )
        val prices = mapOf(NativeAssetId.BITCOIN to price(NativeAssetId.BITCOIN, 50_000_000_000L))
        assertEquals(0L, HoldingsValueCalculator.holdingUsdMicros(holding, prices))
    }

    @Test
    fun totalSumsPricedHoldingsAndSkipsMissingPrices() {
        val holdings = listOf(
            PortfolioHolding(
                destination = PortfolioHoldingDestination.Bitcoin,
                headline = "Bitcoin (BTC)",
                nativeSymbol = "BTC",
                balanceSatoshis = 100_000_000L,
            ),
            PortfolioHolding(
                destination = PortfolioHoldingDestination.Evm(EvmFamily.BSC),
                headline = "BNB Smart Chain (BNB)",
                nativeSymbol = "BNB",
                balanceWei = BigInteger("1000000000000000000"),
            ),
        )
        val prices = mapOf(NativeAssetId.BITCOIN to price(NativeAssetId.BITCOIN, 10_000_000_000L))

        assertEquals(10_000_000_000L, HoldingsValueCalculator.totalUsdMicros(holdings, prices))
    }

    @Test
    fun totalIsNullWhenNoHoldingsHavePrices() {
        val holdings = listOf(
            PortfolioHolding(
                destination = PortfolioHoldingDestination.Bitcoin,
                headline = "Bitcoin (BTC)",
                nativeSymbol = "BTC",
                balanceSatoshis = 1L,
            ),
        )
        assertNull(HoldingsValueCalculator.totalUsdMicros(holdings, emptyMap()))
    }

    @Test
    fun bitcoinWalletUsdUsesConfirmedSatoshis() {
        val prices = mapOf(NativeAssetId.BITCOIN to price(NativeAssetId.BITCOIN, 20_000_000_000L))
        assertEquals(10_000_000L, HoldingsValueCalculator.bitcoinUsdMicros(50_000L, prices))
        assertNull(HoldingsValueCalculator.bitcoinUsdMicros(null, prices))
        assertNull(HoldingsValueCalculator.bitcoinUsdMicros(50_000L, emptyMap()))
    }

    @Test
    fun evmWalletUsdUsesFamilyNativeAsset() {
        val prices = mapOf(NativeAssetId.BINANCECOIN to price(NativeAssetId.BINANCECOIN, 600_000_000L))
        assertEquals(
            600_000_000L,
            HoldingsValueCalculator.evmUsdMicros("1000000000000000000", EvmFamily.BSC, prices),
        )
        assertNull(HoldingsValueCalculator.evmUsdMicros(null, EvmFamily.BSC, prices))
        assertNull(HoldingsValueCalculator.evmUsdMicros("1000000000000000000", null, prices))
    }

    private fun price(id: NativeAssetId, micros: Long) = AssetPrice(
        assetId = id,
        priceUsdMicros = micros,
        updatedAtMillis = 1_700_000_000_000L,
    )
}
