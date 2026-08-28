package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor

/** Marks that transactions for this wallet have been fetched, even if the list is empty. */
@Entity(
    tableName = "ethereum_wallet_tx_cache",
    foreignKeys = [
        ForeignKey(
            entity = EthereumWalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EthereumWalletTxCacheEntity(
    @PrimaryKey val walletId: String,
    val nextCursorJson: String?,
    val hasMore: Boolean,
    val fetchedAtMillis: Long,
)

fun EthereumWalletTxCacheEntity.nextCursor(json: Json): EvmTransactionPaginationCursor? =
    nextCursorJson?.let { json.decodeFromString(it) }

fun EvmTransactionPaginationCursor.toJson(json: Json): String =
    json.encodeToString(this)
