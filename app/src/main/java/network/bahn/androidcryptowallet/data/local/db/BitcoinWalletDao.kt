package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BitcoinWalletDao {
    @Query("SELECT * FROM bitcoin_wallet WHERE network = :network ORDER BY id")
    fun observeByNetwork(network: String): Flow<List<BitcoinWalletEntity>>

    @Query("SELECT * FROM bitcoin_wallet WHERE id = :id")
    fun observeById(id: String): Flow<BitcoinWalletEntity?>

    @Query(
        "SELECT * FROM bitcoin_wallet WHERE network = :network AND receiveAddress = :receiveAddress LIMIT 1",
    )
    suspend fun findByNetworkAndAddress(network: String, receiveAddress: String): BitcoinWalletEntity?

    @Query("SELECT id FROM bitcoin_wallet WHERE id LIKE 'mock:%'")
    suspend fun mockWalletIds(): List<String>

    @Insert
    suspend fun insert(entity: BitcoinWalletEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: BitcoinWalletEntity)

    @Query("DELETE FROM bitcoin_wallet WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM bitcoin_wallet WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        UPDATE bitcoin_wallet
        SET confirmedBalanceSatoshis = :confirmedSatoshis,
            unconfirmedBalanceSatoshis = :unconfirmedSatoshis,
            balanceUpdatedAtMillis = :updatedAtMillis
        WHERE id = :id
        """,
    )
    suspend fun updateBalance(
        id: String,
        confirmedSatoshis: Long,
        unconfirmedSatoshis: Long,
        updatedAtMillis: Long,
    )

    @Query("UPDATE bitcoin_wallet SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String?)
}
