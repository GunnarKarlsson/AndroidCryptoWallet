package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BitcoinNetworkStatusEntity::class,
        BitcoinWalletEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun bitcoinNetworkStatusDao(): BitcoinNetworkStatusDao
    abstract fun bitcoinWalletDao(): BitcoinWalletDao
}
