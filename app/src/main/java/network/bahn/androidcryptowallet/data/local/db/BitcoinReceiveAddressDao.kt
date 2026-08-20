package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BitcoinReceiveAddressDao {
    @Query("SELECT * FROM bitcoin_receive_address WHERE network = :network")
    fun observe(network: String): Flow<BitcoinReceiveAddressEntity?>

    @Query("SELECT COUNT(*) FROM bitcoin_receive_address")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(entity: BitcoinReceiveAddressEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BitcoinReceiveAddressEntity>)
}
