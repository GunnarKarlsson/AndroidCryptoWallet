package network.bahn.androidcryptowallet.data.remote.coingecko

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import network.bahn.androidcryptowallet.domain.model.NativeAssetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinGeckoPriceRemoteDataSourceTest {
    @Test
    fun usdToPriceMicrosScalesAndRoundsHalfUp() {
        assertEquals(65_000_120_000L, CoinGeckoPriceRemoteDataSource.usdToPriceMicros(65_000.12))
        assertEquals(1_234_568L, CoinGeckoPriceRemoteDataSource.usdToPriceMicros(1.2345675))
        assertEquals(1_234_567L, CoinGeckoPriceRemoteDataSource.usdToPriceMicros(1.2345674))
    }

    @Test
    fun parseUsdMicrosMapsKnownIdsAndIgnoresUnknown() {
        val response = buildJsonObject {
            put("bitcoin", buildJsonObject { put("usd", 100_000.0) })
            put("ethereum", buildJsonObject { put("usd", 3_500.5) })
            put("not-a-coin", buildJsonObject { put("usd", 1.0) })
        }

        val prices = CoinGeckoPriceRemoteDataSource.parseUsdMicros(response)

        assertEquals(100_000_000_000L, prices[NativeAssetId.BITCOIN])
        assertEquals(3_500_500_000L, prices[NativeAssetId.ETHEREUM])
        assertEquals(2, prices.size)
    }

    @Test
    fun parseUsdMicrosSkipsMissingUsdQuote() {
        val response = buildJsonObject {
            put("bitcoin", buildJsonObject { put("eur", 90_000.0) })
        }
        assertTrue(CoinGeckoPriceRemoteDataSource.parseUsdMicros(response).isEmpty())
    }

    @Test
    fun fetchUsdMicrosUsesCoinGeckoIds() = runTest {
        val api = FakeCoinGeckoApi(
            response = buildJsonObject {
                put("bitcoin", buildJsonObject { put("usd", 65_000.0) })
            },
        )
        val remote = CoinGeckoPriceRemoteDataSource(api)

        val prices = remote.fetchUsdMicros()

        assertEquals(65_000_000_000L, prices[NativeAssetId.BITCOIN])
        assertTrue(api.lastIds!!.contains("bitcoin"))
        assertTrue(api.lastIds!!.contains("ethereum"))
    }
}

private class FakeCoinGeckoApi(
    var response: JsonObject = buildJsonObject { },
) : CoinGeckoApi {
    var lastIds: String? = null

    override suspend fun getSimplePrice(
        ids: String,
        vsCurrencies: String,
    ): JsonObject {
        lastIds = ids
        return response
    }
}
