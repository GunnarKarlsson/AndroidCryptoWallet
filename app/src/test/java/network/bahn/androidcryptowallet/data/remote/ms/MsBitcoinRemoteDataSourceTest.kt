package network.bahn.androidcryptowallet.data.remote.ms

import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class MsBitcoinRemoteDataSourceTest {
    @Test
    fun toBalanceUsesChainAndMempoolStats() {
        val response = MsAddressResponse(
            chainStats = MsAddressStats(fundedTxoSum = 10_000L, spentTxoSum = 3_000L),
            mempoolStats = MsAddressStats(fundedTxoSum = 500L, spentTxoSum = 100L),
        )
        val balance = response.toBalance()
        assertEquals(7_000L, balance.confirmedSatoshis)
        assertEquals(400L, balance.unconfirmedSatoshis)
    }

    @Test
    fun toBalanceTreatsMissingStatsAsZero() {
        val balance = MsAddressResponse().toBalance()
        assertEquals(0L, balance.confirmedSatoshis)
        assertEquals(0L, balance.unconfirmedSatoshis)
    }

    @Test
    fun parseTipHeightReadsPlainIntegerBody() {
        assertEquals(149_361L, parseMsTipHeight("149361"))
        assertEquals(963_379L, parseMsTipHeight("963379\n"))
    }

    @Test
    fun parseTipHeightRejectsEmptyBody() {
        try {
            parseMsTipHeight("  \n")
            error("expected failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("empty tip height"))
        }
    }

    @Test
    fun heightUrlDiffersByNetwork() {
        val config = TEST_CONFIG
        assertEquals(
            "https://mempool.space/testnet4/api/blocks/tip/height",
            config.heightUrl(BitcoinNetwork.TESTNET4),
        )
        assertEquals(
            "https://mempool.space/api/v1/blocks/tip/height",
            config.heightUrl(BitcoinNetwork.MAINNET),
        )
    }

    @Test
    fun notFoundAddressIsZeroBalance() = runTest {
        val api = FakeMsApi(addressError = httpError(404))
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val balance = remote.getAddressBalance(BitcoinNetwork.TESTNET4, "tb1qmissing")

        assertEquals(0L, balance.confirmedSatoshis)
        assertEquals(0L, balance.unconfirmedSatoshis)
    }

    @Test
    fun getBlockCountParsesTipHeightBody() = runTest {
        val api = FakeMsApi(heightBody = "149361\n")
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val height = remote.getBlockCount(BitcoinNetwork.TESTNET4)

        assertEquals(149_361L, height)
        assertEquals(
            "https://mempool.space/testnet4/api/blocks/tip/height",
            api.lastHeightUrl,
        )
    }
}

private val TEST_CONFIG = MsBitcoinConfig(
    testnet4BaseUrl = "https://mempool.space/testnet4/api/",
    mainnetBaseUrl = "https://mempool.space/api/",
)

private fun httpError(code: Int): HttpException {
    val body = "".toResponseBody("text/plain".toMediaType())
    return HttpException(Response.error<MsAddressResponse>(code, body))
}

private class FakeMsApi(
    private val addressResponse: MsAddressResponse? = null,
    private val addressError: HttpException? = null,
    private val heightBody: String = "0",
) : MsApi {
    var lastHeightUrl: String? = null

    override suspend fun getAddress(address: String): MsAddressResponse {
        addressError?.let { throw it }
        return addressResponse ?: error("unused")
    }

    override suspend fun getTipHeight(url: String): ResponseBody {
        lastHeightUrl = url
        return heightBody.toResponseBody("text/plain".toMediaType())
    }
}
