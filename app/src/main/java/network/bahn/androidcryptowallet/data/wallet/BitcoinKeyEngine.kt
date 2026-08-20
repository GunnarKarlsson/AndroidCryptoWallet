package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress

/**
 * Bitcoin key operations. Implementation uses BDK; domain/UI must not import
 * `org.bitcoindevkit`.
 */
interface BitcoinKeyEngine {
    /** BIP-39: new 12-word English mnemonic. */
    fun generateMnemonic(): List<String>

    /** BIP-39: wordlist + checksum. Throws [network.bahn.androidcryptowallet.domain.model.InvalidBitcoinMnemonicException]. */
    fun validateMnemonic(words: List<String>)

    /**
     * BIP-32 HD derivation under BIP-84 Native SegWit. Returns the external
     * (receive) address at index 0 for both networks.
     */
    fun deriveReceiveAddresses(
        mnemonicWords: List<String>,
        passphrase: String?,
    ): List<BitcoinReceiveAddress>
}
