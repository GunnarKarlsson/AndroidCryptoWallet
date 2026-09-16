package network.bahn.androidcryptowallet.di

import okhttp3.OkHttpClient

internal object OkHttpDebugLogging {
    fun install(builder: OkHttpClient.Builder) = Unit
}
