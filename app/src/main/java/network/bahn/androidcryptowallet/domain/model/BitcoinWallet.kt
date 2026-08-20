package network.bahn.androidcryptowallet.domain.model

/**
 * Local Bitcoin wallet metadata. The BIP-39 mnemonic lives only in the encrypted store;
 * this type is public state (whether a wallet exists / has a passphrase).
 */
data class BitcoinWallet(
    val hasPassphrase: Boolean,
)
