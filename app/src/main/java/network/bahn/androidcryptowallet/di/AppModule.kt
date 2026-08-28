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
import network.bahn.androidcryptowallet.data.local.db.EthereumTransactionDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.db.WALLET_MIGRATION_8_9
import network.bahn.androidcryptowallet.data.local.db.WALLET_MIGRATION_9_10
import network.bahn.androidcryptowallet.data.local.db.WALLET_MIGRATION_10_11
import network.bahn.androidcryptowallet.data.local.db.WalletDatabase
import network.bahn.androidcryptowallet.data.remote.evm.EvmChainCatalog
import network.bahn.androidcryptowallet.data.remote.evm.EvmExplorerEndpoint
import network.bahn.androidcryptowallet.data.remote.evm.EvmExplorerKind
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
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
            .addMigrations(WALLET_MIGRATION_8_9, WALLET_MIGRATION_9_10, WALLET_MIGRATION_10_11)
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
    fun provideEthereumTransactionDao(database: WalletDatabase): EthereumTransactionDao =
        database.ethereumTransactionDao()

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
    fun provideEvmChainCatalog(): EvmChainCatalog = EvmChainCatalog(
        rpcUrls = mapOf(
            EvmNetwork.SEPOLIA to "https://ethereum-sepolia-rpc.publicnode.com",
            EvmNetwork.MAINNET to "https://ethereum.publicnode.com",
            EvmNetwork.BSC_TESTNET to "https://data-seed-prebsc-1-s1.bnbchain.org:8545",
            EvmNetwork.BSC_MAINNET to "https://bsc-dataseed.bnbchain.org",
            EvmNetwork.POLYGON_AMOY to "https://polygon-amoy-bor-rpc.publicnode.com",
            EvmNetwork.POLYGON_MAINNET to "https://polygon-bor-rpc.publicnode.com",
            EvmNetwork.ARBITRUM_SEPOLIA to "https://sepolia-rollup.arbitrum.io/rpc",
            EvmNetwork.ARBITRUM_MAINNET to "https://arb1.arbitrum.io/rpc",
            EvmNetwork.BASE_SEPOLIA to "https://sepolia.base.org",
            EvmNetwork.BASE_MAINNET to "https://mainnet.base.org",
            EvmNetwork.OPTIMISM_SEPOLIA to "https://sepolia.optimism.io",
            EvmNetwork.OPTIMISM_MAINNET to "https://mainnet.optimism.io",
            EvmNetwork.AVALANCHE_FUJI to "https://api.avax-test.network/ext/bc/C/rpc",
            EvmNetwork.AVALANCHE_MAINNET to "https://api.avax.network/ext/bc/C/rpc",
        ),
        explorerEndpoints = mapOf(
            EvmNetwork.SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://eth-sepolia.blockscout.com/api/v2",
                kind = EvmExplorerKind.BLOCKSCOUT,
            ),
            EvmNetwork.MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://eth.blockscout.com/api/v2",
                kind = EvmExplorerKind.BLOCKSCOUT,
            ),
            EvmNetwork.BSC_TESTNET to EvmExplorerEndpoint(
                baseUrl = "https://api-testnet.bscscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BSC_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.bscscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.POLYGON_AMOY to EvmExplorerEndpoint(
                baseUrl = "https://api-amoy.polygonscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.POLYGON_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.polygonscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.ARBITRUM_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia.arbiscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.ARBITRUM_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.arbiscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BASE_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia.basescan.org/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BASE_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.basescan.org/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.OPTIMISM_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia-optimistic.etherscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.OPTIMISM_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api-optimistic.etherscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.AVALANCHE_FUJI to EvmExplorerEndpoint(
                baseUrl = "https://api-testnet.snowtrace.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.AVALANCHE_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.snowtrace.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
        ),
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
