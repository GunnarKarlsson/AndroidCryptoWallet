package network.bahn.androidcryptowallet.domain.model

/**
 * Public Bitcoin wallet row. BIP-39 mnemonic is in the encrypted store, keyed by [id].
 * [receiveAddress] is BIP-84 Native SegWit at index [derivationIndex] for [network].
 */
data class BitcoinWallet(
    val id: String,
    val network: BitcoinNetwork,
    val receiveAddress: String,
    val derivationIndex: Int = 0,
    val scriptType: BitcoinScriptType = BitcoinScriptType.BIP84,
)
