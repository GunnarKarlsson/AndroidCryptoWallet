package network.bahn.androidcryptowallet.data.remote.ms

import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun firstPageHitsTxsEndpoint() = runTest {
        val api = FakeMsApi(txs = listOf(mempoolTx("unconfirmed"), confirmedTx("confirmed")))
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val page = remote.getAddressTransactions(BitcoinNetwork.TESTNET4, ADDRESS, afterTxid = null)

        assertEquals(listOf(ADDRESS), api.txsAddresses)
        assertTrue(api.chainRequests.isEmpty())
        assertEquals(listOf("unconfirmed", "confirmed"), page.transactions.map { it.txid })
        assertEquals("confirmed", page.lastConfirmedTxid)
        assertFalse(page.hasMore)
    }

    @Test
    fun nextPageHitsChainEndpoint() = runTest {
        val api = FakeMsApi(chainTxs = List(MS_TX_PAGE_SIZE) { confirmedTx("c$it") })
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val page = remote.getAddressTransactions(
            BitcoinNetwork.TESTNET4,
            ADDRESS,
            afterTxid = "last-confirmed",
        )

        assertTrue(api.txsAddresses.isEmpty())
        assertEquals(listOf(ADDRESS to "last-confirmed"), api.chainRequests)
        assertTrue(page.hasMore)
        assertEquals("c${MS_TX_PAGE_SIZE - 1}", page.lastConfirmedTxid)
    }

    @Test
    fun notFoundAddressIsEmptyTransactions() = runTest {
        val api = FakeMsApi(txsError = httpError(404))
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val page = remote.getAddressTransactions(BitcoinNetwork.TESTNET4, "tb1qmissing", null)

        assertTrue(page.transactions.isEmpty())
        assertNull(page.lastConfirmedTxid)
        assertFalse(page.hasMore)
    }
}

private val TEST_CONFIG = MsBitcoinConfig(
    testnet4BaseUrl = "https://mempool.space/testnet4/api/",
    mainnetBaseUrl = "https://mempool.space/api/",
)

private const val ADDRESS = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34"

private fun httpError(code: Int): HttpException {
    val body = "".toResponseBody("text/plain".toMediaType())
    return HttpException(Response.error<MsAddressResponse>(code, body))
}

private class FakeMsApi(
    private val addressResponse: MsAddressResponse? = null,
    private val addressError: HttpException? = null,
    private val heightBody: String = "0",
    private val txs: List<MsTxResponse> = emptyList(),
    private val chainTxs: List<MsTxResponse> = emptyList(),
    private val txsError: HttpException? = null,
) : MsApi {
    var lastHeightUrl: String? = null
    val txsAddresses = mutableListOf<String>()
    val chainRequests = mutableListOf<Pair<String, String>>()

    override suspend fun getAddress(address: String): MsAddressResponse {
        addressError?.let { throw it }
        return addressResponse ?: error("unused")
    }

    override suspend fun getAddressTransactions(address: String): List<MsTxResponse> {
        txsError?.let { throw it }
        txsAddresses += address
        return txs
    }

    override suspend fun getAddressTransactionsChain(
        address: String,
        lastTxid: String,
    ): List<MsTxResponse> {
        txsError?.let { throw it }
        chainRequests += address to lastTxid
        return chainTxs
    }

    override suspend fun getTipHeight(url: String): ResponseBody {
        lastHeightUrl = url
        return heightBody.toResponseBody("text/plain".toMediaType())
    }
}
