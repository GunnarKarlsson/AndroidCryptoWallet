package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EvmTransactionDao {
    @Query(
        """
        SELECT
            t.walletId AS walletId,
            t.hash AS hash,
            t.confirmed AS confirmed,
            t.blockTimeSeconds AS blockTimeSeconds,
            t.netWei AS netWei,
            t.feeWei AS feeWei,
            t.sortIndex AS sortIndex,
            w.name AS walletName,
            w.network AS walletNetwork
        FROM ethereum_transaction t
        INNER JOIN ethereum_wallet w ON t.walletId = w.id
        """,
    )
    fun observeAllWithWallet(): Flow<List<EvmTransactionWithWalletRow>>

    @Query("SELECT * FROM ethereum_transaction WHERE walletId = :walletId ORDER BY sortIndex ASC")
    suspend fun listByWalletId(walletId: String): List<EvmTransactionEntity>

    @Query("SELECT COALESCE(MAX(sortIndex), -1) FROM ethereum_transaction WHERE walletId = :walletId")
    suspend fun maxSortIndex(walletId: String): Int

    @Query("SELECT * FROM ethereum_wallet_tx_cache WHERE walletId = :walletId")
    suspend fun cacheForWallet(walletId: String): EvmWalletTxCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(entities: List<EvmTransactionEntity>)

    @Query("DELETE FROM ethereum_transaction WHERE walletId = :walletId")
    suspend fun deleteByWalletId(walletId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCache(entity: EvmWalletTxCacheEntity)

    @Transaction
    suspend fun replaceWalletTransactions(
        walletId: String,
        transactions: List<EvmTransactionEntity>,
        cache: EvmWalletTxCacheEntity,
    ) {
        deleteByWalletId(walletId)
        if (transactions.isNotEmpty()) {
            upsertTransactions(transactions)
        }
        upsertCache(cache)
    }

    @Transaction
    suspend fun appendWalletTransactions(
        transactions: List<EvmTransactionEntity>,
        cache: EvmWalletTxCacheEntity,
    ) {
        if (transactions.isNotEmpty()) {
            upsertTransactions(transactions)
        }
        upsertCache(cache)
    }
}
