package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BitcoinNetworkStatusDao {
    @Query("SELECT * FROM bitcoin_network_status WHERE network = :network")
    fun observe(network: String): Flow<BitcoinNetworkStatusEntity?>

    @Upsert
    suspend fun upsert(entity: BitcoinNetworkStatusEntity)
}
