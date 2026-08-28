package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import network.bahn.androidcryptowallet.domain.model.EvmTransactionSummary

@Entity(
    tableName = "ethereum_transaction",
    primaryKeys = ["walletId", "hash"],
    foreignKeys = [
        ForeignKey(
            entity = EthereumWalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["walletId"])],
)
data class EthereumTransactionEntity(
    val walletId: String,
    val hash: String,
    val confirmed: Boolean,
    val blockTimeSeconds: Long?,
    val netWei: String,
    val feeWei: String?,
    val sortIndex: Int,
)

fun EthereumTransactionEntity.toDomain(): EvmTransactionSummary = EvmTransactionSummary(
    hash = hash,
    confirmed = confirmed,
    blockTimeSeconds = blockTimeSeconds,
    netWei = netWei,
    feeWei = feeWei,
)

fun EvmTransactionSummary.toEntity(
    walletId: String,
    sortIndex: Int,
): EthereumTransactionEntity = EthereumTransactionEntity(
    walletId = walletId,
    hash = hash,
    confirmed = confirmed,
    blockTimeSeconds = blockTimeSeconds,
    netWei = netWei,
    feeWei = feeWei,
    sortIndex = sortIndex,
)
