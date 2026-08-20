package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinNetworkStatus

@Entity(tableName = "bitcoin_network_status")
data class BitcoinNetworkStatusEntity(
    @PrimaryKey val network: String,
    val blockHeight: Long,
    val updatedAtMillis: Long,
)

fun BitcoinNetworkStatusEntity.toDomain(): BitcoinNetworkStatus = BitcoinNetworkStatus(
    network = BitcoinNetwork.valueOf(network),
    blockHeight = blockHeight,
    updatedAtMillis = updatedAtMillis,
)
