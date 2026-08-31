package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EvmWalletDao {
    @Query("SELECT id FROM ethereum_wallet WHERE network = :network")
    suspend fun listIdsByNetwork(network: String): List<String>

    @Query("SELECT id FROM ethereum_wallet")
    suspend fun listAllIds(): List<String>

    @Query("SELECT * FROM ethereum_wallet WHERE network = :network ORDER BY id")
    fun observeByNetwork(network: String): Flow<List<EvmWalletEntity>>

    @Query("SELECT * FROM ethereum_wallet WHERE id = :id")
    fun observeById(id: String): Flow<EvmWalletEntity?>

    @Query(
        "SELECT * FROM ethereum_wallet WHERE network = :network AND address = :address LIMIT 1",
    )
    suspend fun findByNetworkAndAddress(network: String, address: String): EvmWalletEntity?

    @Insert
    suspend fun insert(entity: EvmWalletEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: EvmWalletEntity)

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
