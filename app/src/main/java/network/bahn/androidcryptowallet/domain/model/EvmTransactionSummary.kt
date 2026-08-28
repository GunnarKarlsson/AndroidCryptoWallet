package network.bahn.androidcryptowallet.domain.model

data class EvmTransactionSummary(
    val hash: String,
    val confirmed: Boolean,
    val blockTimeSeconds: Long?,
    val netWei: String,
    val feeWei: String?,
)
