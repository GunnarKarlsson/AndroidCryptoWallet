package network.bahn.androidcryptowallet.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkDataStore
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkDataStore
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.local.secure.EncryptedBitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.local.secure.EncryptedEthereumMnemonicStore
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.ms.MsApiFactory
import network.bahn.androidcryptowallet.data.remote.ms.MsApiProvider
import network.bahn.androidcryptowallet.data.remote.ms.MsBitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.repository.BitcoinNetworkStatusRepositoryImpl
import network.bahn.androidcryptowallet.data.repository.BitcoinWalletRepositoryImpl
import network.bahn.androidcryptowallet.data.repository.EthereumWalletRepositoryImpl
import network.bahn.androidcryptowallet.data.wallet.BdkBitcoinKeyEngine
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.data.wallet.EthereumKeyEngine
import network.bahn.androidcryptowallet.data.wallet.WalletCatalogInitializer
import network.bahn.androidcryptowallet.data.wallet.Web3jEthereumKeyEngine
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
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
    abstract fun bindSelectedEthereumNetworkStore(
        impl: SelectedEthereumNetworkDataStore,
    ): SelectedEthereumNetworkStore

    @Binds
    @Singleton
    abstract fun bindEthereumKeyEngine(
        impl: Web3jEthereumKeyEngine,
    ): EthereumKeyEngine

    @Binds
    @Singleton
    abstract fun bindEthereumMnemonicStore(
        impl: EncryptedEthereumMnemonicStore,
    ): EthereumMnemonicStore

    @Binds
    @Singleton
    abstract fun bindEthereumWalletRepository(
        impl: EthereumWalletRepositoryImpl,
    ): EthereumWalletRepository

    @Binds
    @Singleton
    abstract fun bindWalletCatalogReadiness(
        impl: WalletCatalogInitializer,
    ): WalletCatalogReadiness
}
