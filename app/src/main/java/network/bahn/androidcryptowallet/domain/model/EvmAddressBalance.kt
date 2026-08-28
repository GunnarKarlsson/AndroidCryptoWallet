package network.bahn.androidcryptowallet.domain.model

/** On-chain balance from `eth_getBalance` in decimal wei (not hex). */
data class EvmAddressBalance(
    val balanceWei: String,
)
