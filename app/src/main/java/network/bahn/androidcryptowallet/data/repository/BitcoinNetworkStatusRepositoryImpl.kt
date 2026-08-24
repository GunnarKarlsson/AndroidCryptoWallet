package network.bahn.androidcryptowallet.data.repository

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.data.local.db.BitcoinNetworkStatusDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinNetworkStatusEntity
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinNetworkStatus
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BitcoinNetworkStatusRepositoryImpl @Inject constructor(
    private val dao: BitcoinNetworkStatusDao,
    private val selectedBitcoinNetworkStore: SelectedBitcoinNetworkStore,
    private val remote: BitcoinRemoteDataSource,
    private val timeProvider: TimeProvider,
) : BitcoinNetworkStatusRepository {
    override fun selectedNetwork(): Flow<BitcoinNetwork> =
        selectedBitcoinNetworkStore.selectedNetwork

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStatus(): Flow<BitcoinNetworkStatus?> =
        selectedBitcoinNetworkStore.selectedNetwork.flatMapLatest { network ->
            dao.observe(network.name).map { entity -> entity?.toDomain() }
        }

    override suspend fun setNetwork(network: BitcoinNetwork) {
        selectedBitcoinNetworkStore.setNetwork(network)
    }

    override suspend fun refreshBlockHeight() {
        val network = selectedBitcoinNetworkStore.selectedNetwork.first()
        val height = remote.getBlockCount(network)
        dao.upsert(
            BitcoinNetworkStatusEntity(
                network = network.name,
                blockHeight = height,
                updatedAtMillis = timeProvider.nowMillis(),
            ),
        )
        Log.i(TAG, "Stored block height $height for $network")
    }

    private companion object {
        const val TAG = "NetworkStatus"
    }
}
