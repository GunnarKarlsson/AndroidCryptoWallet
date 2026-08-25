package network.bahn.androidcryptowallet.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.BuildConfig
import network.bahn.androidcryptowallet.data.local.db.BitcoinNetworkStatusDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinTransactionDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.db.WALLET_MIGRATION_8_9
import network.bahn.androidcryptowallet.data.local.db.WalletDatabase
import network.bahn.androidcryptowallet.data.remote.ms.MsBitcoinConfig
import network.bahn.androidcryptowallet.data.wallet.MockBitcoinWalletConfig
import network.bahn.androidcryptowallet.domain.TimeProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideWalletDatabase(@ApplicationContext context: Context): WalletDatabase =
        Room.databaseBuilder(context, WalletDatabase::class.java, "wallet.db")
            .addMigrations(WALLET_MIGRATION_8_9)
            .build()

    @Provides
    fun provideBitcoinNetworkStatusDao(database: WalletDatabase): BitcoinNetworkStatusDao =
        database.bitcoinNetworkStatusDao()

    @Provides
    fun provideBitcoinWalletDao(database: WalletDatabase): BitcoinWalletDao =
        database.bitcoinWalletDao()

    @Provides
    fun provideBitcoinTransactionDao(database: WalletDatabase): BitcoinTransactionDao =
        database.bitcoinTransactionDao()

    @Provides
    fun provideEthereumWalletDao(database: WalletDatabase): EthereumWalletDao =
        database.ethereumWalletDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("settings")
        }

    @Provides
    @Singleton
    fun provideMockBitcoinWalletConfig(): MockBitcoinWalletConfig = MockBitcoinWalletConfig.fromRaw(
        testnet4Raw = BuildConfig.MOCK_BITCOIN_WALLET_TESTNET4,
        mainnetRaw = BuildConfig.MOCK_BITCOIN_WALLET_MAINNET,
    )

    @Provides
    @Singleton
    fun provideMsBitcoinConfig(): MsBitcoinConfig = MsBitcoinConfig(
        testnet4BaseUrl = BuildConfig.MS_TESTNET4_BASE_URL,
        mainnetBaseUrl = BuildConfig.MS_MAINNET_BASE_URL,
    )

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message ->
                Log.d("OkHttp", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = TimeProvider { System.currentTimeMillis() }
}
