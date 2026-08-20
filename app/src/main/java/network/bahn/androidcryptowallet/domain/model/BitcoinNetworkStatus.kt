package network.bahn.androidcryptowallet.domain.model

data class BitcoinNetworkStatus(
    val network: BitcoinNetwork,
    val blockHeight: Long,
    val updatedAtMillis: Long,
)
