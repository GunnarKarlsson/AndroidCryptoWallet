package network.bahn.androidcryptowallet.data.remote.alchemy

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AlchemyJsonRpcApi {
    @POST("{apiKey}")
    suspend fun call(
        @Path("apiKey") apiKey: String,
        @Body body: JsonRpcRequest,
    ): JsonRpcResponse
}
