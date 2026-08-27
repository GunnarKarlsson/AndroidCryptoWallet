package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface EthereumTransactionDao {
    @Query("SELECT * FROM ethereum_transaction WHERE walletId = :walletId ORDER BY sortIndex ASC")
    suspend fun listByWalletId(walletId: String): List<EthereumTransactionEntity>

    @Query("SELECT COALESCE(MAX(sortIndex), -1) FROM ethereum_transaction WHERE walletId = :walletId")
    suspend fun maxSortIndex(walletId: String): Int

    @Query("SELECT * FROM ethereum_wallet_tx_cache WHERE walletId = :walletId")
    suspend fun cacheForWallet(walletId: String): EthereumWalletTxCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(entities: List<EthereumTransactionEntity>)

    @Query("DELETE FROM ethereum_transaction WHERE walletId = :walletId")
    suspend fun deleteByWalletId(walletId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCache(entity: EthereumWalletTxCacheEntity)

    @Transaction
    suspend fun replaceWalletTransactions(
        walletId: String,
        transactions: List<EthereumTransactionEntity>,
        cache: EthereumWalletTxCacheEntity,
    ) {
        deleteByWalletId(walletId)
        if (transactions.isNotEmpty()) {
            upsertTransactions(transactions)
        }
        upsertCache(cache)
    }

    @Transaction
    suspend fun appendWalletTransactions(
        transactions: List<EthereumTransactionEntity>,
        cache: EthereumWalletTxCacheEntity,
    ) {
        if (transactions.isNotEmpty()) {
            upsertTransactions(transactions)
        }
        upsertCache(cache)
    }
}
