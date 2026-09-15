package network.bahn.androidcryptowallet.domain.model

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object HoldingsValueCalculator {
    private val SATOSHIS_PER_BTC = BigDecimal("100000000")
    private val WEI_PER_COIN = BigDecimal.TEN.pow(18)

    fun holdingUsdMicros(
        holding: PortfolioHolding,
        prices: Map<NativeAssetId, AssetPrice>,
    ): Long? {
        val price = prices[holding.destination.nativeAssetId()] ?: return null
        return when {
            holding.balanceSatoshis != null ->
                satoshisToUsdMicros(holding.balanceSatoshis, price.priceUsdMicros)
            holding.balanceWei != null ->
                weiToUsdMicros(holding.balanceWei, price.priceUsdMicros)
            else -> null
        }
    }

    fun totalUsdMicros(
        holdings: List<PortfolioHolding>,
        prices: Map<NativeAssetId, AssetPrice>,
    ): Long? {
        var anyPriced = false
        var total = 0L
        for (holding in holdings) {
            val micros = holdingUsdMicros(holding, prices) ?: continue
            anyPriced = true
            total += micros
        }
        return total.takeIf { anyPriced }
    }

    fun bitcoinUsdMicros(
        satoshis: Long?,
        prices: Map<NativeAssetId, AssetPrice>,
    ): Long? {
        val amount = satoshis ?: return null
        val price = prices[NativeAssetId.BITCOIN] ?: return null
        return satoshisToUsdMicros(amount, price.priceUsdMicros)
    }

    fun evmUsdMicros(
        wei: String?,
        family: EvmFamily?,
        prices: Map<NativeAssetId, AssetPrice>,
    ): Long? {
        val assetFamily = family ?: return null
        val amount = wei?.toBigIntegerOrNull() ?: return null
        val price = prices[assetFamily.nativeAssetId()] ?: return null
        return weiToUsdMicros(amount, price.priceUsdMicros)
    }

    internal fun satoshisToUsdMicros(satoshis: Long, priceUsdMicros: Long): Long =
        BigDecimal.valueOf(satoshis)
            .multiply(BigDecimal.valueOf(priceUsdMicros))
            .divide(SATOSHIS_PER_BTC, 0, RoundingMode.HALF_UP)
            .longValueExact()

    internal fun weiToUsdMicros(wei: BigInteger, priceUsdMicros: Long): Long =
        wei.toBigDecimal()
            .multiply(BigDecimal.valueOf(priceUsdMicros))
            .divide(WEI_PER_COIN, 0, RoundingMode.HALF_UP)
            .longValueExact()
}
