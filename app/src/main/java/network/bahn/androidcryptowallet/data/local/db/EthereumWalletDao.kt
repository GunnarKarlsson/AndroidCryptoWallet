package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EthereumWalletDao {
    @Query("SELECT * FROM ethereum_wallet WHERE network = :network ORDER BY id")
    fun observeByNetwork(network: String): Flow<List<EthereumWalletEntity>>

    @Query(
        "SELECT * FROM ethereum_wallet WHERE network = :network AND address = :address LIMIT 1",
    )
    suspend fun findByNetworkAndAddress(network: String, address: String): EthereumWalletEntity?

    @Insert
    suspend fun insert(entity: EthereumWalletEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: EthereumWalletEntity)
}
