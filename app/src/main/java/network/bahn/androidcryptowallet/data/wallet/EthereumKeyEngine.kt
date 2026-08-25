package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.EthereumReceiveAddress

/**
 * Ethereum key operations. Implementation uses BDK for BIP-39 generate and web3j for
 * BIP-44 derivation; domain/UI must not import those libraries.
 */
interface EthereumKeyEngine {
    /** BIP-39: new 12-word English mnemonic. */
    fun generateMnemonic(): List<String>

    /** BIP-39: wordlist + checksum. Throws [network.bahn.androidcryptowallet.domain.model.InvalidEthereumMnemonicException]. */
    fun validateMnemonic(words: List<String>)

    /**
     * BIP-44 HD derivation `m/44'/60'/0'/0/0`. Address is the same on Sepolia and Mainnet.
     */
    fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
    ): EthereumReceiveAddress
}
