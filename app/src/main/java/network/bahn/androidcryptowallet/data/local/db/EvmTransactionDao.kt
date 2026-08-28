package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface EvmTransactionDao {
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
