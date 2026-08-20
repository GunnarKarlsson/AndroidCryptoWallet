package network.bahn.androidcryptowallet.data.remote.alchemy

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
class AlchemyUtxoApiFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val config: AlchemyBitcoinConfig,
) {
    private val jsonMediaType = "application/json".toMediaType()
    private val apis = ConcurrentHashMap<BitcoinNetwork, AlchemyUtxoApi>()

    fun get(network: BitcoinNetwork): AlchemyUtxoApi =
        apis.getOrPut(network) {
            Retrofit.Builder()
                .baseUrl(config.baseUrl(network))
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(jsonMediaType))
                .build()
                .create(AlchemyUtxoApi::class.java)
        }
}
