package network.bahn.androidcryptowallet.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkDataStore
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.alchemy.AlchemyBitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.repository.BitcoinNetworkStatusRepositoryImpl
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
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
    abstract fun bindBitcoinRemoteDataSource(
        impl: AlchemyBitcoinRemoteDataSource,
    ): BitcoinRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindBitcoinNetworkStatusRepository(
        impl: BitcoinNetworkStatusRepositoryImpl,
    ): BitcoinNetworkStatusRepository
}
