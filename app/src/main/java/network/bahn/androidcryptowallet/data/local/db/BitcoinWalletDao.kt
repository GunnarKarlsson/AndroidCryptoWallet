package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BitcoinWalletDao {
    @Query("SELECT * FROM bitcoin_wallet WHERE network = :network ORDER BY id")
    fun observeByNetwork(network: String): Flow<List<BitcoinWalletEntity>>

    @Insert
    suspend fun insert(entity: BitcoinWalletEntity)
}
