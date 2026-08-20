package network.bahn.androidcryptowallet.domain.model

/**
 * Public receive address derived with [scriptType].
 *
 * For [BitcoinScriptType.BIP84] this is Native SegWit at
 * `m/84'/0'/0'/0/{index}` (mainnet `bc1q`) or `m/84'/1'/0'/0/{index}` (testnet4 `tb1q`).
 * Private keys are not stored here.
 */
data class BitcoinReceiveAddress(
    val network: BitcoinNetwork,
    val address: String,
    val index: Int,
    val scriptType: BitcoinScriptType = BitcoinScriptType.BIP84,
)
