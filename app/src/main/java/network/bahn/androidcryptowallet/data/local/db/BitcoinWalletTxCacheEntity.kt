package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Marks that transactions for this wallet have been fetched, even if the list is empty. */
@Entity(
    tableName = "bitcoin_wallet_tx_cache",
    foreignKeys = [
        ForeignKey(
            entity = BitcoinWalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BitcoinWalletTxCacheEntity(
    @PrimaryKey val walletId: String,
    val lastConfirmedTxid: String?,
    val hasMore: Boolean,
    val fetchedAtMillis: Long,
)
