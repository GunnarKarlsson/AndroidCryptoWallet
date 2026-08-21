package network.bahn.androidcryptowallet.domain.model

/**
 * How a wallet row was created.
 * [HD] has a BIP-39 mnemonic in the encrypted store.
 * [WATCH_ONLY] is an imported public address with no keys.
 */
enum class BitcoinWalletKind {
    HD,
    WATCH_ONLY,
}
