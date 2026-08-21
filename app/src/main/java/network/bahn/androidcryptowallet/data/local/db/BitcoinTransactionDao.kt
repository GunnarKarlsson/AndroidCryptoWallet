package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface BitcoinTransactionDao {
    @Query("SELECT * FROM bitcoin_transaction WHERE walletId = :walletId ORDER BY sortIndex ASC")
    suspend fun listByWalletId(walletId: String): List<BitcoinTransactionEntity>

    @Query("SELECT COALESCE(MAX(sortIndex), -1) FROM bitcoin_transaction WHERE walletId = :walletId")
    suspend fun maxSortIndex(walletId: String): Int

    @Query("SELECT * FROM bitcoin_wallet_tx_cache WHERE walletId = :walletId")
    suspend fun cacheForWallet(walletId: String): BitcoinWalletTxCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(entities: List<BitcoinTransactionEntity>)

    @Query("DELETE FROM bitcoin_transaction WHERE walletId = :walletId")
    suspend fun deleteByWalletId(walletId: String)

    @Upsert
    suspend fun upsertCache(entity: BitcoinWalletTxCacheEntity)

    @Transaction
    suspend fun replaceWalletTransactions(
        walletId: String,
        transactions: List<BitcoinTransactionEntity>,
        cache: BitcoinWalletTxCacheEntity,
    ) {
        deleteByWalletId(walletId)
        if (transactions.isNotEmpty()) {
            upsertTransactions(transactions)
        }
        upsertCache(cache)
    }

    @Transaction
    suspend fun appendWalletTransactions(
        transactions: List<BitcoinTransactionEntity>,
        cache: BitcoinWalletTxCacheEntity,
    ) {
        if (transactions.isNotEmpty()) {
            upsertTransactions(transactions)
        }
        upsertCache(cache)
    }
}
