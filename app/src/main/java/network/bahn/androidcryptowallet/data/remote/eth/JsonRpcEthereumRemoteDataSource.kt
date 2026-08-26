package network.bahn.androidcryptowallet.data.remote.eth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.data.remote.EthereumRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.EthereumAddressBalance
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonRpcEthereumRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
    private val config: EthereumRpcConfig,
    private val json: Json,
) : EthereumRemoteDataSource {
    override suspend fun getAddressBalance(
        network: EthereumNetwork,
        address: String,
    ): EthereumAddressBalance = withContext(Dispatchers.IO) {
        Log.d(TAG, "Requesting eth_getBalance for $network")
        val requestBody = json.encodeToString(
            JsonRpcRequest(
                method = "eth_getBalance",
                params = listOf(address, "latest"),
            ),
        )
        val httpRequest = Request.Builder()
            .url(config.rpcUrl(network))
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val responseBody = client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                error("RPC HTTP ${response.code}")
            }
            response.body?.string() ?: error("Empty RPC response")
        }
        val rpcResponse = json.decodeFromString<JsonRpcResponse>(responseBody)
        rpcResponse.error?.let { error ->
            error("RPC error ${error.code}: ${error.message}")
        }
        val hexWei = rpcResponse.result ?: error("Missing RPC result")
        val balanceWei = parseHexWei(hexWei)
        Log.i(TAG, "eth_getBalance succeeded for $network wei=$balanceWei")
        EthereumAddressBalance(balanceWei = balanceWei)
    }

    @Serializable
    private data class JsonRpcRequest(
        @SerialName("jsonrpc") val jsonRpc: String = "2.0",
        val method: String,
        val params: List<String>,
        val id: Int = 1,
    )

    @Serializable
    private data class JsonRpcResponse(
        val result: String? = null,
        val error: JsonRpcError? = null,
    )

    @Serializable
    private data class JsonRpcError(
        val code: Int,
        val message: String,
    )

    companion object {
        private const val TAG = "EthRemote"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        internal fun parseHexWei(hex: String): String {
            val clean = hex.removePrefix("0x").ifEmpty { "0" }
            return BigInteger(clean, 16).toString()
        }
    }
}
