package network.bahn.androidcryptowallet.domain.model

/** Live EIP-1559 fee oracle values from the node (decimal wei strings). */
data class EvmFeeData(
    val baseFeePerGasWei: String,
    val suggestedPriorityFeePerGasWei: String,
)
