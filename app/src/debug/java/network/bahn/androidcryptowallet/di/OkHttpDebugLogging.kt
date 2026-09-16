package network.bahn.androidcryptowallet.di

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Debug-only HTTP logging. Never use [HttpLoggingInterceptor.Level.BODY]:
 * `eth_sendRawTransaction` and Bitcoin broadcast bodies are signed raw hex.
 */
internal object OkHttpDebugLogging {
    fun install(builder: OkHttpClient.Builder) {
        builder.addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
    }
}
