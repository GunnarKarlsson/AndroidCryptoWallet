package network.bahn.androidcryptowallet.data.remote.ms

import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MsApiFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val config: MsBitcoinConfig,
) : MsApiProvider {
    private val jsonMediaType = "application/json".toMediaType()
    private val apis = ConcurrentHashMap<BitcoinNetwork, MsApi>()

    override fun get(network: BitcoinNetwork): MsApi =
        apis.getOrPut(network) {
            Retrofit.Builder()
                .baseUrl(config.baseUrl(network))
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(jsonMediaType))
                .build()
                .create(MsApi::class.java)
        }
}
