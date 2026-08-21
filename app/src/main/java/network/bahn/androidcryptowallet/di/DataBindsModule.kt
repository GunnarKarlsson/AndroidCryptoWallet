package network.bahn.androidcryptowallet.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkDataStore
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.local.secure.EncryptedBitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.remote.ms.MsApiFactory
import network.bahn.androidcryptowallet.data.remote.ms.MsApiProvider
import network.bahn.androidcryptowallet.data.repository.BitcoinNetworkStatusRepositoryImpl
import network.bahn.androidcryptowallet.data.repository.BitcoinWalletRepositoryImpl
import network.bahn.androidcryptowallet.data.wallet.BdkBitcoinKeyEngine
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
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
}
