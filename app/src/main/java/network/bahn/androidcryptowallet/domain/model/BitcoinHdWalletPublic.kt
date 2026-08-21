package network.bahn.androidcryptowallet.domain.model

/** Public HD wallet fields stored next to the mnemonic so Room can be rebuilt. */
data class BitcoinHdWalletPublic(
    val id: String,
    val network: BitcoinNetwork,
    val receiveAddress: String,
    val derivationIndex: Int = 0,
    val scriptType: BitcoinScriptType = BitcoinScriptType.BIP84,
)
