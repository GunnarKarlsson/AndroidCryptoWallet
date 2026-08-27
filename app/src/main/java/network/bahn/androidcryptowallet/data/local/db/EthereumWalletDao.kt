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

    @Query("SELECT * FROM ethereum_wallet WHERE id = :id")
    fun observeById(id: String): Flow<EthereumWalletEntity?>

    @Query(
        "SELECT * FROM ethereum_wallet WHERE network = :network AND address = :address LIMIT 1",
    )
    suspend fun findByNetworkAndAddress(network: String, address: String): EthereumWalletEntity?

    @Insert
    suspend fun insert(entity: EthereumWalletEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: EthereumWalletEntity)

    @Query("DELETE FROM ethereum_wallet WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        UPDATE ethereum_wallet
        SET balanceWei = :balanceWei,
            balanceUpdatedAtMillis = :updatedAtMillis
        WHERE id = :id
        """,
    )
    suspend fun updateBalance(
        id: String,
        balanceWei: String,
        updatedAtMillis: Long,
    )

    @Query("UPDATE ethereum_wallet SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String?)
}
