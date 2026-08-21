package network.bahn.androidcryptowallet.domain.model

/**
 * Bitcoin script / address type. [BIP84] is Native SegWit (`bc1q` / `tb1q`).
 * [EXTERNAL] is an imported address whose script was not derived (watch-only).
 * BIP-86 Taproot is out of scope for this milestone.
 */
enum class BitcoinScriptType {
    BIP84,
    EXTERNAL,
}
