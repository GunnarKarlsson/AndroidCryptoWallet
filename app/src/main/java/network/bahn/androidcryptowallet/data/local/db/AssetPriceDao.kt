package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetPriceDao {
    @Query("SELECT * FROM asset_price")
    fun observeAll(): Flow<List<AssetPriceEntity>>

    @Query("SELECT * FROM asset_price WHERE assetId IN (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<AssetPriceEntity>>

    @Upsert
    suspend fun upsertAll(entities: List<AssetPriceEntity>)
}
