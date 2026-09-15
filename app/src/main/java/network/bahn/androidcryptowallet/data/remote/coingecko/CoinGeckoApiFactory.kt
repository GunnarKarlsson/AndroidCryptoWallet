package network.bahn.androidcryptowallet.data.remote.coingecko

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinGeckoApiFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    fun create(): CoinGeckoApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                okHttpClient.newBuilder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("Accept", "application/json")
                                .header("User-Agent", USER_AGENT)
                                .build(),
                        )
                    }
                    .build(),
            )
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(CoinGeckoApi::class.java)

    private companion object {
        const val BASE_URL = "https://api.coingecko.com/api/v3/"
        const val USER_AGENT = "AndroidCryptoWallet/1.0"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
