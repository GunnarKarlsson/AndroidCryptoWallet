package network.bahn.androidcryptowallet.domain.model

data class BitcoinUtxo(
    val txid: String,
    val vout: Int,
    val valueSatoshis: Long,
    val confirmed: Boolean,
)
