package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType

/** Public BIP-84 receive address cache. No secrets. */
@Entity(tableName = "bitcoin_receive_address")
data class BitcoinReceiveAddressEntity(
    @PrimaryKey val network: String,
    val address: String,
    val derivationIndex: Int,
    val scriptType: String,
)

fun BitcoinReceiveAddressEntity.toDomain(): BitcoinReceiveAddress = BitcoinReceiveAddress(
    network = BitcoinNetwork.valueOf(network),
    address = address,
    index = derivationIndex,
    scriptType = BitcoinScriptType.valueOf(scriptType),
)

fun BitcoinReceiveAddress.toEntity(): BitcoinReceiveAddressEntity = BitcoinReceiveAddressEntity(
    network = network.name,
    address = address,
    derivationIndex = index,
    scriptType = scriptType.name,
)
