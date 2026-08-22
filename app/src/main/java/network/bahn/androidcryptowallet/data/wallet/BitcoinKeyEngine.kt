package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinSignedTransaction

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
     * BIP-32 HD derivation under BIP-84 Native SegWit. External (receive) address
     * at index 0 for [network] only.
     */
    fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
        network: BitcoinNetwork,
    ): BitcoinReceiveAddress

    fun isValidAddress(
        network: BitcoinNetwork,
        address: String,
    ): Boolean

    /**
     * Build and sign a payment to [recipientAddress] from an in-memory BIP-84 wallet.
     * [fundingTxHexes] are raw previous transactions that pay the wallet's receive script.
     * Change is sent to [changeAddress] (the same receive address in the single-address model).
     */
    fun buildAndSignSend(
        mnemonicWords: List<String>,
        passphrase: String?,
        network: BitcoinNetwork,
        fundingTxHexes: List<String>,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
        changeAddress: String,
    ): BitcoinSignedTransaction
}
