package network.bahn.androidcryptowallet.data.remote.alchemy

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AlchemyUtxoApi {
    @GET("{apiKey}/api/v2/address/{address}")
    suspend fun getAddress(
        @Path("apiKey") apiKey: String,
        @Path("address") address: String,
        @Query("details") details: String = "basic",
    ): AlchemyAddressResponse
}

@Serializable
data class AlchemyAddressResponse(
    val address: String? = null,
    val balance: String? = null,
    val unconfirmedBalance: String? = null,
)
