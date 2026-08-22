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

    @Test
    fun utxoDtoMapsConfirmedFlag() {
        val confirmed = MsUtxoResponse(
            txid = "aa",
            vout = 1,
            value = 50_000L,
            status = MsUtxoStatus(confirmed = true),
        ).toDomain()
        val mempool = MsUtxoResponse(
            txid = "bb",
            vout = 0,
            value = 1_000L,
            status = MsUtxoStatus(confirmed = false),
        ).toDomain()
        val missing = MsUtxoResponse(txid = "cc", vout = 0, value = 2L).toDomain()

        assertEquals("aa", confirmed.txid)
        assertEquals(1, confirmed.vout)
        assertEquals(50_000L, confirmed.valueSatoshis)
        assertTrue(confirmed.confirmed)
        assertFalse(mempool.confirmed)
        assertFalse(missing.confirmed)
    }

    @Test
    fun notFoundAddressIsEmptyUtxos() = runTest {
        val api = FakeMsApi(utxosError = httpError(404))
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val utxos = remote.getAddressUtxos(BitcoinNetwork.TESTNET4, "tb1qmissing")

        assertTrue(utxos.isEmpty())
    }

    @Test
    fun getAddressUtxosMapsResponses() = runTest {
        val api = FakeMsApi(
            utxos = listOf(
                MsUtxoResponse("txid-1", 0, 12_345L, MsUtxoStatus(confirmed = true)),
            ),
        )
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val utxos = remote.getAddressUtxos(BitcoinNetwork.TESTNET4, ADDRESS)

        assertEquals(listOf(ADDRESS), api.utxoAddresses)
        assertEquals(1, utxos.size)
        assertEquals("txid-1", utxos.single().txid)
        assertTrue(utxos.single().confirmed)
    }

    @Test
    fun getTransactionHexReturnsPlainBody() = runTest {
        val api = FakeMsApi(txHexBody = "02000000\n")
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val hex = remote.getTransactionHex(BitcoinNetwork.TESTNET4, "txid-1")

        assertEquals("02000000", hex)
        assertEquals(listOf("txid-1"), api.txHexIds)
    }

    @Test
    fun broadcastPostsHexAndReturnsTxid() = runTest {
        val api = FakeMsApi(broadcastTxid = "broadcast-txid\n")
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        val txid = remote.broadcastTransaction(BitcoinNetwork.TESTNET4, "02000000dead")

        assertEquals("broadcast-txid", txid)
        assertEquals("02000000dead", api.lastBroadcastHex)
    }

    @Test
    fun broadcastNon2xxUsesErrorBody() = runTest {
        val api = FakeMsApi(broadcastError = httpError(400, "txn-mempool-conflict"))
        val remote = MsBitcoinRemoteDataSource(
            apiProvider = MsApiProvider { api },
            config = TEST_CONFIG,
        )

        try {
            remote.broadcastTransaction(BitcoinNetwork.TESTNET4, "02000000")
            error("expected failure")
        } catch (e: IllegalStateException) {
            assertEquals("txn-mempool-conflict", e.message)
        }
    }
}

private val TEST_CONFIG = MsBitcoinConfig(
    testnet4BaseUrl = "https://mempool.space/testnet4/api/",
    mainnetBaseUrl = "https://mempool.space/api/",
)

private const val ADDRESS = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34"

private fun httpError(code: Int, body: String = ""): HttpException {
    val responseBody = body.toResponseBody("text/plain".toMediaType())
    return HttpException(Response.error<MsAddressResponse>(code, responseBody))
}

private class FakeMsApi(
    private val addressResponse: MsAddressResponse? = null,
    private val addressError: HttpException? = null,
    private val heightBody: String = "0",
    private val txs: List<MsTxResponse> = emptyList(),
    private val chainTxs: List<MsTxResponse> = emptyList(),
    private val txsError: HttpException? = null,
    private val utxos: List<MsUtxoResponse> = emptyList(),
    private val utxosError: HttpException? = null,
    private val txHexBody: String = "",
    private val broadcastTxid: String = "txid",
    private val broadcastError: HttpException? = null,
) : MsApi {
    var lastHeightUrl: String? = null
    val txsAddresses = mutableListOf<String>()
    val chainRequests = mutableListOf<Pair<String, String>>()
    val utxoAddresses = mutableListOf<String>()
    val txHexIds = mutableListOf<String>()
    var lastBroadcastHex: String? = null

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

    override suspend fun getAddressUtxos(address: String): List<MsUtxoResponse> {
        utxosError?.let { throw it }
        utxoAddresses += address
        return utxos
    }

    override suspend fun getTransactionHex(txid: String): ResponseBody {
        txHexIds += txid
        return txHexBody.toResponseBody("text/plain".toMediaType())
    }

    override suspend fun broadcastTransaction(rawTxHex: okhttp3.RequestBody): ResponseBody {
        broadcastError?.let { throw it }
        val buffer = okio.Buffer()
        rawTxHex.writeTo(buffer)
        lastBroadcastHex = buffer.readUtf8()
        return broadcastTxid.toResponseBody("text/plain".toMediaType())
    }

    override suspend fun getTipHeight(url: String): ResponseBody {
        lastHeightUrl = url
        return heightBody.toResponseBody("text/plain".toMediaType())
    }
}
