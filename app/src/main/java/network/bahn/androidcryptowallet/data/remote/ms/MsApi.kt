package network.bahn.androidcryptowallet.data.remote.ms

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bahn.androidcryptowallet.domain.model.BitcoinAddressBalance
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

fun interface MsApiProvider {
    fun get(network: BitcoinNetwork): MsApi
}

interface MsApi {
    @GET("address/{address}")
    suspend fun getAddress(
        @Path("address") address: String,
    ): MsAddressResponse

    @GET("address/{address}/txs")
    suspend fun getAddressTransactions(
        @Path("address") address: String,
    ): List<MsTxResponse>

    @GET("address/{address}/txs/chain/{lastTxid}")
    suspend fun getAddressTransactionsChain(
        @Path("address") address: String,
        @Path("lastTxid") lastTxid: String,
    ): List<MsTxResponse>

    @GET
    suspend fun getTipHeight(
        @Url url: String,
    ): ResponseBody
}

@Serializable
data class MsAddressResponse(
    val address: String? = null,
    @SerialName("chain_stats") val chainStats: MsAddressStats? = null,
    @SerialName("mempool_stats") val mempoolStats: MsAddressStats? = null,
)

@Serializable
data class MsAddressStats(
    @SerialName("funded_txo_sum") val fundedTxoSum: Long = 0L,
    @SerialName("spent_txo_sum") val spentTxoSum: Long = 0L,
)

fun MsAddressResponse.toBalance(): BitcoinAddressBalance {
    val chain = chainStats ?: MsAddressStats()
    val mempool = mempoolStats ?: MsAddressStats()
    return BitcoinAddressBalance(
        confirmedSatoshis = (chain.fundedTxoSum - chain.spentTxoSum).coerceAtLeast(0L),
        unconfirmedSatoshis = mempool.fundedTxoSum - mempool.spentTxoSum,
    )
}

fun parseMsTipHeight(body: String): Long =
    body.trim().toLongOrNull() ?: error("empty tip height")

fun isMsAddressNotFound(httpCode: Int): Boolean = httpCode == 404 || httpCode == 400
