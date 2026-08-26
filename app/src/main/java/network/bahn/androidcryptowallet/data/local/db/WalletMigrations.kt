package network.bahn.androidcryptowallet.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive only: creates [ethereum_wallet]. Must not drop or rewrite Bitcoin tables.
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
 * Additive only: balance columns on [ethereum_wallet]. Must not alter Bitcoin tables.
 */
val WALLET_MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ethereum_wallet ADD COLUMN balanceWei TEXT")
        db.execSQL("ALTER TABLE ethereum_wallet ADD COLUMN balanceUpdatedAtMillis INTEGER")
    }
}
