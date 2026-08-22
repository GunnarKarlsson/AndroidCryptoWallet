package network.bahn.androidcryptowallet.domain.model

/**
 * Public Bitcoin wallet row.
 * [HD][BitcoinWalletKind.HD] wallets keep a BIP-39 mnemonic in the encrypted store, keyed by [id];
 * [receiveAddress] is BIP-84 Native SegWit at index [derivationIndex] for [network].
 * [WATCH_ONLY][BitcoinWalletKind.WATCH_ONLY] wallets have no keys; [scriptType] is [EXTERNAL][BitcoinScriptType.EXTERNAL].
 */
data class BitcoinWallet(
    val id: String,
    val network: BitcoinNetwork,
    val receiveAddress: String,
    val derivationIndex: Int = 0,
    val scriptType: BitcoinScriptType = BitcoinScriptType.BIP84,
    val kind: BitcoinWalletKind = BitcoinWalletKind.HD,
    val confirmedBalanceSatoshis: Long? = null,
    val unconfirmedBalanceSatoshis: Long? = null,
    val balanceUpdatedAtMillis: Long? = null,
    val name: String? = null,
)
