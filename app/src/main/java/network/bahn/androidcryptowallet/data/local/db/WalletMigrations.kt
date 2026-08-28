package network.bahn.androidcryptowallet.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive only: creates `ethereum_wallet` ([EvmWalletEntity]). Must not drop or rewrite Bitcoin tables.
 */
val WALLET_MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ethereum_wallet (
              id TEXT NOT NULL PRIMARY KEY,
              network TEXT NOT NULL,
              address TEXT NOT NULL,
              derivationIndex INTEGER NOT NULL,
              name TEXT
            )
            """.trimIndent(),
        )
    }
}

/**
 * Additive only: balance columns on `ethereum_wallet` ([EvmWalletEntity]). Must not alter Bitcoin tables.
 */
val WALLET_MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ethereum_wallet ADD COLUMN balanceWei TEXT")
        db.execSQL("ALTER TABLE ethereum_wallet ADD COLUMN balanceUpdatedAtMillis INTEGER")
    }
}

/**
 * Additive only: EVM transaction tables ([EvmTransactionEntity], [EvmWalletTxCacheEntity]).
 * Must not alter Bitcoin tables.
 */
val WALLET_MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ethereum_transaction (
              walletId TEXT NOT NULL,
              hash TEXT NOT NULL,
              confirmed INTEGER NOT NULL,
              blockTimeSeconds INTEGER,
              netWei TEXT NOT NULL,
              feeWei TEXT,
              sortIndex INTEGER NOT NULL,
              PRIMARY KEY(walletId, hash),
              FOREIGN KEY(walletId) REFERENCES ethereum_wallet(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ethereum_transaction_walletId ON ethereum_transaction(walletId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ethereum_wallet_tx_cache (
              walletId TEXT NOT NULL PRIMARY KEY,
              nextCursorJson TEXT,
              hasMore INTEGER NOT NULL,
              fetchedAtMillis INTEGER NOT NULL,
              FOREIGN KEY(walletId) REFERENCES ethereum_wallet(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
    }
}
