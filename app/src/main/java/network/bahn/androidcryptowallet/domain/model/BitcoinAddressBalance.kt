package network.bahn.androidcryptowallet.domain.model

data class BitcoinAddressBalance(
    val confirmedSatoshis: Long,
    val unconfirmedSatoshis: Long = 0L,
)
