package network.bahn.androidcryptowallet.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkDataStore
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEvmNetworkDataStore
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEvmNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.local.secure.EncryptedBitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.local.secure.EncryptedEvmMnemonicStore
import network.bahn.androidcryptowallet.data.local.secure.EvmMnemonicStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.EvmRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.blockscout.RoutingEvmTransactionRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.blockscout.EvmTransactionRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.eth.JsonRpcEvmRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.ms.MsApiFactory
import network.bahn.androidcryptowallet.data.remote.ms.MsApiProvider
import network.bahn.androidcryptowallet.data.remote.ms.MsBitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.repository.BitcoinNetworkStatusRepositoryImpl
import network.bahn.androidcryptowallet.data.repository.BitcoinWalletRepositoryImpl
import network.bahn.androidcryptowallet.data.repository.EvmWalletRepositoryImpl
import network.bahn.androidcryptowallet.data.wallet.BdkBitcoinKeyEngine
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.data.wallet.EvmKeyEngine
import network.bahn.androidcryptowallet.data.wallet.WalletCatalogInitializer
import network.bahn.androidcryptowallet.data.wallet.Web3jEvmKeyEngine
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import network.bahn.androidcryptowallet.ui.chain.ContextEvmFamilyDefaultNames
import network.bahn.androidcryptowallet.ui.chain.EvmFamilyDefaultNames
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds
    @Singleton
    abstract fun bindSelectedBitcoinNetworkStore(
        impl: SelectedBitcoinNetworkDataStore,
    ): SelectedBitcoinNetworkStore

    @Binds
    @Singleton
    abstract fun bindMsApiProvider(
        impl: MsApiFactory,
    ): MsApiProvider

    @Binds
    @Singleton
    abstract fun bindBitcoinRemoteDataSource(
        impl: MsBitcoinRemoteDataSource,
    ): BitcoinRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindEvmRemoteDataSource(
        impl: JsonRpcEvmRemoteDataSource,
    ): EvmRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindEvmTransactionRemoteDataSource(
        impl: RoutingEvmTransactionRemoteDataSource,
    ): EvmTransactionRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindBitcoinNetworkStatusRepository(
        impl: BitcoinNetworkStatusRepositoryImpl,
    ): BitcoinNetworkStatusRepository

    @Binds
    @Singleton
    abstract fun bindBitcoinKeyEngine(
        impl: BdkBitcoinKeyEngine,
    ): BitcoinKeyEngine

    @Binds
    @Singleton
    abstract fun bindBitcoinMnemonicStore(
        impl: EncryptedBitcoinMnemonicStore,
    ): BitcoinMnemonicStore

    @Binds
    @Singleton
    abstract fun bindBitcoinWalletRepository(
        impl: BitcoinWalletRepositoryImpl,
    ): BitcoinWalletRepository

    @Binds
    @Singleton
    abstract fun bindSelectedEvmNetworkStore(
        impl: SelectedEvmNetworkDataStore,
    ): SelectedEvmNetworkStore

    @Binds
    @Singleton
    abstract fun bindEvmKeyEngine(
        impl: Web3jEvmKeyEngine,
    ): EvmKeyEngine

    @Binds
    @Singleton
    abstract fun bindEvmMnemonicStore(
        impl: EncryptedEvmMnemonicStore,
    ): EvmMnemonicStore

    @Binds
    @Singleton
    abstract fun bindEvmWalletRepository(
        impl: EvmWalletRepositoryImpl,
    ): EvmWalletRepository

    @Binds
    @Singleton
    abstract fun bindEvmFamilyDefaultNames(
        impl: ContextEvmFamilyDefaultNames,
    ): EvmFamilyDefaultNames

    @Binds
    @Singleton
    abstract fun bindWalletCatalogReadiness(
        impl: WalletCatalogInitializer,
    ): WalletCatalogReadiness
}
