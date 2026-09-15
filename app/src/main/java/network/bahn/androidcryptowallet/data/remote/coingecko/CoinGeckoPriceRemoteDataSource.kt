package network.bahn.androidcryptowallet.data.remote.coingecko

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import network.bahn.androidcryptowallet.data.remote.AssetPriceRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.NativeAssetId
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinGeckoPriceRemoteDataSource @Inject constructor(
    private val api: CoinGeckoApi,
) : AssetPriceRemoteDataSource {
    override suspend fun fetchUsdMicros(): Map<NativeAssetId, Long> {
        val idParam = NativeAssetId.entries.joinToString(",") { geckoIdFor(it) }
        val response = api.getSimplePrice(ids = idParam)
        return parseUsdMicros(response)
    }

    companion object {
        fun parseUsdMicros(response: JsonObject): Map<NativeAssetId, Long> = buildMap {
            for ((geckoId, quotesElement) in response) {
                val asset = assetForGeckoId(geckoId) ?: continue
                val quotes = quotesElement as? JsonObject ?: continue
                val usd = quotes["usd"]?.jsonPrimitive?.doubleOrNull ?: continue
                put(asset, usdToPriceMicros(usd))
            }
        }

        fun usdToPriceMicros(usd: Double): Long =
            BigDecimal.valueOf(usd)
                .movePointRight(6)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
    }
}

private val geckoIds: Map<NativeAssetId, String> = mapOf(
    NativeAssetId.BITCOIN to "bitcoin",
    NativeAssetId.ETHEREUM to "ethereum",
    NativeAssetId.BINANCECOIN to "binancecoin",
    NativeAssetId.POLYGON to "polygon-ecosystem-token",
    NativeAssetId.AVALANCHE to "avalanche-2",
)

private val assetsByGeckoId: Map<String, NativeAssetId> =
    geckoIds.entries.associate { (asset, geckoId) -> geckoId to asset }

private fun geckoIdFor(asset: NativeAssetId): String = geckoIds.getValue(asset)

private fun assetForGeckoId(id: String): NativeAssetId? = assetsByGeckoId[id]
