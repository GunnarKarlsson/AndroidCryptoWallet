package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BitcoinNetworkStatusEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun bitcoinNetworkStatusDao(): BitcoinNetworkStatusDao
}
