package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet

/** Public Ethereum wallet row. No BIP-39 secrets. */
@Entity(tableName = "ethereum_wallet")
data class EthereumWalletEntity(
    @PrimaryKey val id: String,
    val network: String,
    val address: String,
    val derivationIndex: Int,
    val name: String? = null,
    val balanceWei: String? = null,
    val balanceUpdatedAtMillis: Long? = null,
)

fun EthereumWalletEntity.toDomain(): EthereumWallet = EthereumWallet(
    id = id,
    network = EthereumNetwork.valueOf(network),
    address = address,
    derivationIndex = derivationIndex,
    name = name,
    balanceWei = balanceWei,
    balanceUpdatedAtMillis = balanceUpdatedAtMillis,
)

fun EthereumWallet.toEntity(): EthereumWalletEntity = EthereumWalletEntity(
    id = id,
    network = network.name,
    address = address,
    derivationIndex = derivationIndex,
    name = name,
    balanceWei = balanceWei,
    balanceUpdatedAtMillis = balanceUpdatedAtMillis,
)
