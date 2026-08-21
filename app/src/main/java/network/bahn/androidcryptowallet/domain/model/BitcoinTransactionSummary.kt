package network.bahn.androidcryptowallet.domain.model

data class BitcoinTransactionSummary(
    val txid: String,
    val confirmed: Boolean,
    val blockTimeSeconds: Long?,
    val netSatoshis: Long,
    val feeSatoshis: Long?,
)
