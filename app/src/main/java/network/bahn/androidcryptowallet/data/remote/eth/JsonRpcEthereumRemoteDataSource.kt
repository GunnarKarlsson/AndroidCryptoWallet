package network.bahn.androidcryptowallet.data.remote.eth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import network.bahn.androidcryptowallet.data.remote.EthereumRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.EthereumAddressBalance
import network.bahn.androidcryptowallet.domain.model.EthereumFeeData
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
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
        network: EvmNetwork,
        address: String,
    ): EthereumAddressBalance = withContext(Dispatchers.IO) {
        Log.d(TAG, "Requesting eth_getBalance for $network")
        val hexWei = callHexResult(
            network = network,
            method = "eth_getBalance",
            params = buildJsonArray {
                add(JsonPrimitive(address))
                add(JsonPrimitive("latest"))
            },
        )
        val balanceWei = parseHexQuantity(hexWei)
        Log.i(TAG, "eth_getBalance succeeded for $network wei=$balanceWei")
        EthereumAddressBalance(balanceWei = balanceWei)
    }

    override suspend fun getTransactionCount(
        network: EvmNetwork,
        address: String,
    ): Long = withContext(Dispatchers.IO) {
        Log.d(TAG, "Requesting eth_getTransactionCount for $network")
        val hex = callHexResult(
            network = network,
            method = "eth_getTransactionCount",
            params = buildJsonArray {
                add(JsonPrimitive(address))
                add(JsonPrimitive("pending"))
            },
        )
        parseHexQuantity(hex).toLong()
    }

    override suspend fun estimateGas(
        network: EvmNetwork,
        from: String,
        to: String,
        valueWei: BigInteger,
    ): Long = withContext(Dispatchers.IO) {
        Log.d(TAG, "Requesting eth_estimateGas for $network")
        val callObject = buildJsonObject {
            put("from", from)
            put("to", to)
            put("value", toHexQuantity(valueWei))
        }
        val hex = callHexResult(
            network = network,
            method = "eth_estimateGas",
            params = buildJsonArray { add(callObject) },
        )
        parseHexQuantity(hex).toLong()
    }

    override suspend fun getFeeData(network: EvmNetwork): EthereumFeeData =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Requesting fee data for $network")
            val block = callJsonResult(
                network = network,
                method = "eth_getBlockByNumber",
                params = buildJsonArray {
                    add(JsonPrimitive("latest"))
                    add(JsonPrimitive(false))
                },
            ).jsonObject
            val baseFeeHex = block["baseFeePerGas"]?.jsonPrimitive?.contentOrNull
                ?: error("Missing baseFeePerGas in latest block")
            val priorityHex = callHexResult(
                network = network,
                method = "eth_maxPriorityFeePerGas",
                params = buildJsonArray { },
            )
            EthereumFeeData(
                baseFeePerGasWei = parseHexQuantity(baseFeeHex),
                suggestedPriorityFeePerGasWei = parseHexQuantity(priorityHex),
            )
        }

    override suspend fun sendRawTransaction(
        network: EvmNetwork,
        signedRawHex: String,
    ): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Requesting eth_sendRawTransaction for $network")
        val hex = signedRawHex.let { if (it.startsWith("0x")) it else "0x$it" }
        callHexResult(
            network = network,
            method = "eth_sendRawTransaction",
            params = buildJsonArray { add(JsonPrimitive(hex)) },
        )
    }

    private fun callHexResult(
        network: EvmNetwork,
        method: String,
        params: JsonArray,
    ): String {
        val result = callJsonResult(network, method, params)
        return result.jsonPrimitive.content
    }

    private fun callJsonResult(
        network: EvmNetwork,
        method: String,
        params: JsonArray,
    ): JsonElement {
        val requestBody = json.encodeToString(
            JsonRpcRequest(method = method, params = params),
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
        return rpcResponse.result ?: error("Missing RPC result")
    }

    @Serializable
    private data class JsonRpcRequest(
        @SerialName("jsonrpc") val jsonRpc: String = "2.0",
        val method: String,
        val params: JsonArray,
        val id: Int = 1,
    )

    @Serializable
    private data class JsonRpcResponse(
        val result: JsonElement? = null,
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

        internal fun parseHexQuantity(hex: String): String {
            val clean = hex.removePrefix("0x").removePrefix("0X").ifEmpty { "0" }
            return BigInteger(clean, 16).toString()
        }

        internal fun toHexQuantity(value: BigInteger): String =
            "0x" + value.toString(16)

        /** @deprecated Use [parseHexQuantity]; kept for existing tests. */
        internal fun parseHexWei(hex: String): String = parseHexQuantity(hex)
    }
}
