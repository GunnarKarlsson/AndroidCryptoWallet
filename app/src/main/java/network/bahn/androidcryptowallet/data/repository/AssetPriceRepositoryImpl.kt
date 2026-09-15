package network.bahn.androidcryptowallet.data.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import network.bahn.androidcryptowallet.data.local.db.AssetPriceDao
import network.bahn.androidcryptowallet.data.local.db.AssetPriceEntity
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.data.remote.AssetPriceRemoteDataSource
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.AssetPrice
import network.bahn.androidcryptowallet.domain.model.NativeAssetId
import network.bahn.androidcryptowallet.domain.repository.AssetPriceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetPriceRepositoryImpl @Inject constructor(
    private val dao: AssetPriceDao,
    private val remote: AssetPriceRemoteDataSource,
    private val timeProvider: TimeProvider,
) : AssetPriceRepository {
    override fun observePrices(): Flow<Map<NativeAssetId, AssetPrice>> =
        dao.observeAll().map { entities ->
            entities.mapNotNull { entity ->
                entity.toDomain()?.let { price -> price.assetId to price }
            }.toMap()
        }

    override suspend fun refreshPrices() {
        withContext(Dispatchers.IO) {
            try {
                val fetched = remote.fetchUsdMicros()
                if (fetched.isEmpty()) {
                    Log.w(TAG, "price fetch returned no assets; keeping cached prices")
                    return@withContext
                }
                val now = timeProvider.nowMillis()
                dao.upsertAll(
                    fetched.map { (assetId, micros) ->
                        AssetPriceEntity(
                            assetId = assetId.name,
                            priceUsdMicros = micros,
                            updatedAtMillis = now,
                        )
                    },
                )
                Log.i(TAG, "Stored ${fetched.size} asset prices")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "price refresh failed; keeping cached prices: ${e.message}", e)
            }
        }
    }

    private companion object {
        const val TAG = "AssetPrice"
    }
}
