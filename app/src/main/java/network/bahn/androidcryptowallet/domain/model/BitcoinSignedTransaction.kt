package network.bahn.androidcryptowallet.domain.model

data class BitcoinSignedTransaction(
    val txid: String,
    val rawHex: String,
)
