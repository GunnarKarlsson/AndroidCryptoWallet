package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionSummary

@Entity(
    tableName = "bitcoin_transaction",
    primaryKeys = ["walletId", "txid"],
    foreignKeys = [
        ForeignKey(
            entity = BitcoinWalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["walletId"])],
)
data class BitcoinTransactionEntity(
    val walletId: String,
    val txid: String,
    val confirmed: Boolean,
    val blockTimeSeconds: Long?,
    val netSatoshis: Long,
    val feeSatoshis: Long?,
    val sortIndex: Int,
)

fun BitcoinTransactionEntity.toDomain(): BitcoinTransactionSummary = BitcoinTransactionSummary(
    txid = txid,
    confirmed = confirmed,
    blockTimeSeconds = blockTimeSeconds,
    netSatoshis = netSatoshis,
    feeSatoshis = feeSatoshis,
)

fun BitcoinTransactionSummary.toEntity(
    walletId: String,
    sortIndex: Int,
): BitcoinTransactionEntity = BitcoinTransactionEntity(
    walletId = walletId,
    txid = txid,
    confirmed = confirmed,
    blockTimeSeconds = blockTimeSeconds,
    netSatoshis = netSatoshis,
    feeSatoshis = feeSatoshis,
    sortIndex = sortIndex,
)
