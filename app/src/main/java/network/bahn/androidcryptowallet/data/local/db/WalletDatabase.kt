package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BitcoinNetworkStatusEntity::class,
        BitcoinWalletEntity::class,
        BitcoinTransactionEntity::class,
        BitcoinWalletTxCacheEntity::class,
        EvmWalletEntity::class,
        EvmTransactionEntity::class,
        EvmWalletTxCacheEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun bitcoinNetworkStatusDao(): BitcoinNetworkStatusDao
    abstract fun bitcoinWalletDao(): BitcoinWalletDao
    abstract fun bitcoinTransactionDao(): BitcoinTransactionDao
    abstract fun evmWalletDao(): EvmWalletDao
    abstract fun evmTransactionDao(): EvmTransactionDao
}
