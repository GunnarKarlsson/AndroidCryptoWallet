package network.bahn.androidcryptowallet.data.remote.alchemy

import kotlinx.serialization.Serializable

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String? = null,
    val id: Int? = null,
    val result: Long? = null,
    val error: JsonRpcError? = null,
)

@Serializable
data class JsonRpcError(
    val code: Int? = null,
    val message: String? = null,
)
