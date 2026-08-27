package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.EthereumReceiveAddress
import network.bahn.androidcryptowallet.domain.model.InvalidEthereumMnemonicException
import org.bitcoindevkit.Mnemonic
import org.bitcoindevkit.WordCount
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Web3jEthereumKeyEngine @Inject constructor() : EthereumKeyEngine {
    override fun generateMnemonic(): List<String> {
        // BIP-39: 128 bits of entropy → 12 English words (checksum included).
        val mnemonic = Mnemonic(WordCount.WORDS12)
        return mnemonic.toString().split(" ")
    }

    override fun validateMnemonic(words: List<String>) {
        val normalized = normalize(words)
        if (normalized.size != 12 && normalized.size != 24) {
            throw InvalidEthereumMnemonicException(
                "BIP-39 mnemonic must be 12 or 24 words, found ${normalized.size}",
            )
        }
        val phrase = normalized.joinToString(" ")
        if (!MnemonicUtils.validateMnemonic(phrase)) {
            throw InvalidEthereumMnemonicException("Invalid BIP-39 mnemonic")
        }
    }

    /**
     * Standard Ethereum HD receive address at external index 0.
     *
     * Spec chain (same as MetaMask / Ledger default account):
     * - BIP-39: mnemonic (+ optional passphrase) → seed via PBKDF2
     * - BIP-32: seed → master extended key
     * - BIP-44 path `m/44'/60'/0'/0/0` — purpose 44', coin type 60' (SLIP-0044 Ethereum),
     *   account 0', external chain 0, address index 0
     * - Address: secp256k1 public key → Keccak-256 → last 20 bytes; EIP-55 checksum
     *
     * Sepolia and Mainnet share the same address for a given seed; only the app's network
     * label on the wallet row differs.
     */
    override fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
    ): EthereumReceiveAddress {
        val derived = deriveKeyPair(mnemonicWords, passphrase)
        // EIP-55 mixed-case checksum over the 0x address.
        val checksummed = Keys.toChecksumAddress(Keys.getAddress(derived))
        val address = if (checksummed.startsWith("0x")) checksummed else "0x$checksummed"
        return EthereumReceiveAddress(address = address, index = RECEIVE_INDEX)
    }

    override fun isValidAddress(address: String): Boolean {
        val trimmed = address.trim()
        if (!ADDRESS_PATTERN.matches(trimmed)) return false
        val hex = trimmed.drop(2)
        // Mixed-case addresses must match EIP-55 checksum.
        if (hex.any { it.isLetter() } &&
            hex != hex.lowercase() &&
            hex != hex.uppercase()
        ) {
            return try {
                Keys.toChecksumAddress(trimmed) == trimmed
            } catch (_: Exception) {
                false
            }
        }
        return true
    }

    override fun buildAndSignSend(
        mnemonicWords: List<String>,
        passphrase: String?,
        chainId: Long,
        to: String,
        valueWei: BigInteger,
        nonce: Long,
        gasLimit: Long,
        maxPriorityFeePerGasWei: BigInteger,
        maxFeePerGasWei: BigInteger,
    ): String {
        val keyPair = deriveKeyPair(mnemonicWords, passphrase)
        val credentials = Credentials.create(keyPair)
        val rawTransaction = RawTransaction.createEtherTransaction(
            chainId,
            BigInteger.valueOf(nonce),
            BigInteger.valueOf(gasLimit),
            to,
            valueWei,
            maxPriorityFeePerGasWei,
            maxFeePerGasWei,
        )
        val signed = TransactionEncoder.signMessage(rawTransaction, credentials)
        return Numeric.toHexString(signed)
    }

    private fun deriveKeyPair(
        mnemonicWords: List<String>,
        passphrase: String?,
    ): Bip32ECKeyPair {
        validateMnemonic(mnemonicWords)
        val phrase = normalize(mnemonicWords).joinToString(" ")
        // BIP-39: optional passphrase is the "25th word".
        val seed = MnemonicUtils.generateSeed(phrase, passphrase.orEmpty())
        val master = Bip32ECKeyPair.generateKeyPair(seed)
        // BIP-44: m / purpose' / coin_type' / account' / change / address_index
        val path = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT, // BIP-44
            60 or Bip32ECKeyPair.HARDENED_BIT, // SLIP-0044 coin type: Ethereum
            0 or Bip32ECKeyPair.HARDENED_BIT, // account 0
            0, // external (receive)
            RECEIVE_INDEX,
        )
        return Bip32ECKeyPair.deriveKeyPair(master, path)
    }

    private fun normalize(words: List<String>): List<String> =
        words.map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    private companion object {
        const val RECEIVE_INDEX = 0
        val ADDRESS_PATTERN = Regex("^0[xX][0-9a-fA-F]{40}$")
    }
}
