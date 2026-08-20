package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BitcoinNetworkStatusEntity::class,
        BitcoinReceiveAddressEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun bitcoinNetworkStatusDao(): BitcoinNetworkStatusDao
    abstract fun bitcoinReceiveAddressDao(): BitcoinReceiveAddressDao
}
