package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmWallet

/** Public EVM wallet row. No BIP-39 secrets. Table name stays `ethereum_wallet`. */
@Entity(tableName = "ethereum_wallet")
data class EvmWalletEntity(
    @PrimaryKey val id: String,
    val network: String,
    val address: String,
    val derivationIndex: Int,
    val name: String? = null,
    val balanceWei: String? = null,
    val balanceUpdatedAtMillis: Long? = null,
)

fun EvmWalletEntity.toDomain(): EvmWallet = EvmWallet(
    id = id,
    network = EvmNetwork.valueOf(network),
    address = address,
    derivationIndex = derivationIndex,
    name = name,
    balanceWei = balanceWei,
    balanceUpdatedAtMillis = balanceUpdatedAtMillis,
)

fun EvmWallet.toEntity(): EvmWalletEntity = EvmWalletEntity(
    id = id,
    network = network.name,
    address = address,
    derivationIndex = derivationIndex,
    name = name,
    balanceWei = balanceWei,
    balanceUpdatedAtMillis = balanceUpdatedAtMillis,
)
