package network.bahn.androidcryptowallet.domain.model

/**
 * Public EVM wallet row (any [EvmNetwork] family).
 * HD wallets keep a BIP-39 mnemonic in the encrypted store, keyed by [id];
 * [address] is BIP-44 `m/44'/60'/0'/0/{derivationIndex}` (same address across networks in a family).
 */
data class EvmWallet(
    val id: String,
    val network: EvmNetwork,
    val address: String,
    val derivationIndex: Int = 0,
    val name: String? = null,
    /** Decimal wei string from the last successful balance refresh; null if never fetched. */
    val balanceWei: String? = null,
    val balanceUpdatedAtMillis: Long? = null,
)
