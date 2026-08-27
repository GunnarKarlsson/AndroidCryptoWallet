package network.bahn.androidcryptowallet.domain.model

data class EthereumTransactionSummary(
    val hash: String,
    val confirmed: Boolean,
    val blockTimeSeconds: Long?,
    val netWei: String,
    val feeWei: String?,
)
