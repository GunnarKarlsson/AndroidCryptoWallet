package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet

/** Public wallet row. No BIP-39 secrets. */
@Entity(tableName = "bitcoin_wallet")
data class BitcoinWalletEntity(
    @PrimaryKey val id: String,
    val network: String,
    val receiveAddress: String,
    val derivationIndex: Int,
    val scriptType: String,
)

fun BitcoinWalletEntity.toDomain(): BitcoinWallet = BitcoinWallet(
    id = id,
    network = BitcoinNetwork.valueOf(network),
    receiveAddress = receiveAddress,
    derivationIndex = derivationIndex,
    scriptType = BitcoinScriptType.valueOf(scriptType),
)

fun BitcoinWallet.toEntity(): BitcoinWalletEntity = BitcoinWalletEntity(
    id = id,
    network = network.name,
    receiveAddress = receiveAddress,
    derivationIndex = derivationIndex,
    scriptType = scriptType.name,
)
