package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BitcoinWalletDao {
    @Query("SELECT * FROM bitcoin_wallet WHERE network = :network ORDER BY id")
    fun observeByNetwork(network: String): Flow<List<BitcoinWalletEntity>>

    @Query("SELECT * FROM bitcoin_wallet WHERE id = :id")
    fun observeById(id: String): Flow<BitcoinWalletEntity?>

    @Insert
    suspend fun insert(entity: BitcoinWalletEntity)

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
}
