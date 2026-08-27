package network.bahn.androidcryptowallet.domain.model

/** Resolved EIP-1559 gas parameters for a send (decimal wei strings). */
data class EthereumGasQuote(
    val gasLimit: Long,
    val maxPriorityFeePerGasWei: String,
    val maxFeePerGasWei: String,
    val estimatedFeeWei: String,
)
