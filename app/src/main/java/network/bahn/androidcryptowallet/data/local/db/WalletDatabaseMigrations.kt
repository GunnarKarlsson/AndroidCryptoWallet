package network.bahn.androidcryptowallet.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object WalletDatabaseMigrations {
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bitcoin_transaction` (
                    `walletId` TEXT NOT NULL,
                    `txid` TEXT NOT NULL,
                    `confirmed` INTEGER NOT NULL,
                    `blockTimeSeconds` INTEGER,
                    `netSatoshis` INTEGER NOT NULL,
                    `feeSatoshis` INTEGER,
                    `sortIndex` INTEGER NOT NULL,
                    PRIMARY KEY(`walletId`, `txid`),
                    FOREIGN KEY(`walletId`) REFERENCES `bitcoin_wallet`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_bitcoin_transaction_walletId` ON `bitcoin_transaction` (`walletId`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bitcoin_wallet_tx_cache` (
                    `walletId` TEXT NOT NULL,
                    `lastConfirmedTxid` TEXT,
                    `hasMore` INTEGER NOT NULL,
                    `fetchedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`walletId`),
                    FOREIGN KEY(`walletId`) REFERENCES `bitcoin_wallet`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
        }
    }
}
