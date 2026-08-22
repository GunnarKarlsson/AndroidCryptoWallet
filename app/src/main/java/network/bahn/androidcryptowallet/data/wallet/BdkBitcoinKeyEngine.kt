package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.BitcoinSignedTransaction
import network.bahn.androidcryptowallet.domain.model.InvalidBitcoinMnemonicException
import org.bitcoindevkit.Address
import org.bitcoindevkit.Amount
import org.bitcoindevkit.CreateTxException
import org.bitcoindevkit.Descriptor
import org.bitcoindevkit.DescriptorSecretKey
import org.bitcoindevkit.FeeRate
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Mnemonic
import org.bitcoindevkit.Network
import org.bitcoindevkit.NetworkKind
import org.bitcoindevkit.Persister
import org.bitcoindevkit.Transaction
import org.bitcoindevkit.TxBuilder
import org.bitcoindevkit.UnconfirmedTx
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.WordCount
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BdkBitcoinKeyEngine @Inject constructor() : BitcoinKeyEngine {
    override fun generateMnemonic(): List<String> {
        // BIP-39: 128 bits of entropy → 12 English words (checksum included).
        val mnemonic = Mnemonic(WordCount.WORDS12)
        return mnemonic.toString().split(" ")
    }

    override fun validateMnemonic(words: List<String>) {
        parseMnemonic(words)
    }

    override fun deriveReceiveAddress(
        mnemonicWords: List<String>,
        passphrase: String?,
        network: BitcoinNetwork,
    ): BitcoinReceiveAddress {
        val mnemonic = parseMnemonic(mnemonicWords)
        val password = passphrase?.takeIf { it.isNotEmpty() }
        val wallet = openWallet(mnemonic, password, network)
        val addressInfo = wallet.peekAddress(KeychainKind.EXTERNAL, RECEIVE_INDEX)
        return BitcoinReceiveAddress(
            network = network,
            address = addressInfo.address.toString(),
            index = RECEIVE_INDEX.toInt(),
            scriptType = BitcoinScriptType.BIP84,
        )
    }

    override fun isValidAddress(
        network: BitcoinNetwork,
        address: String,
    ): Boolean {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return false
        return try {
            Address(trimmed, network.toBdkNetwork())
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun buildAndSignSend(
        mnemonicWords: List<String>,
        passphrase: String?,
        network: BitcoinNetwork,
        fundingTxHexes: List<String>,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
        changeAddress: String,
    ): BitcoinSignedTransaction {
        if (fundingTxHexes.isEmpty()) error("Insufficient funds")
        val mnemonic = parseMnemonic(mnemonicWords)
        val password = passphrase?.takeIf { it.isNotEmpty() }
        val wallet = openWallet(mnemonic, password, network)
        wallet.revealNextAddress(KeychainKind.EXTERNAL)
        val lastSeen = (System.currentTimeMillis() / 1_000L).toULong()
        wallet.applyUnconfirmedTxs(
            fundingTxHexes.map { hex ->
                UnconfirmedTx(
                    tx = Transaction(hex.decodeHex()),
                    lastSeen = lastSeen,
                )
            },
        )
        val dest = Address(recipientAddress.trim(), network.toBdkNetwork())
        val change = Address(changeAddress.trim(), network.toBdkNetwork())
        val psbt = try {
            TxBuilder()
                .addRecipient(dest.scriptPubkey(), Amount.fromSat(amountSatoshis.toULong()))
                .feeRate(FeeRate.fromSatPerVb(feeRateSatPerVbyte.toULong()))
                .drainTo(change.scriptPubkey())
                .finish(wallet)
        } catch (e: CreateTxException.InsufficientFunds) {
            throw IllegalStateException("Insufficient funds", e)
        } catch (e: CreateTxException.NoUtxosSelected) {
            throw IllegalStateException("Insufficient funds", e)
        } catch (e: CreateTxException.OutputBelowDustLimit) {
            throw IllegalStateException(BELOW_DUST, e)
        } catch (e: CreateTxException) {
            throw IllegalStateException(
                e.message?.takeIf { it.isNotBlank() } ?: "Could not build transaction",
                e,
            )
        }
        if (!wallet.sign(psbt)) error("Could not sign transaction")
        val tx = psbt.extractTx()
        return BitcoinSignedTransaction(
            txid = tx.computeTxid().toString(),
            rawHex = tx.serialize().toHex(),
        )
    }

    private fun openWallet(
        mnemonic: Mnemonic,
        password: String?,
        network: BitcoinNetwork,
    ): Wallet {
        // BIP-32 extended keys: NetworkKind is MAIN vs TEST (tprv/xprv). Testnet4
        // shares TEST keys with other test networks; Network.TESTNET4 is only for
        // address encoding / genesis (tb1q).
        val networkKind = network.toNetworkKind()
        val secretKey = DescriptorSecretKey(networkKind, mnemonic, password)

        // BIP-84: wpkh(key / 84' / {0,1}' / 0' / {0,1} / *). EXTERNAL = receive /0/*.
        val external = Descriptor.newBip84(secretKey, KeychainKind.EXTERNAL, networkKind)
        // BIP-84 change chain /1/* — drainTo sends change to the receive address instead.
        val internal = Descriptor.newBip84(secretKey, KeychainKind.INTERNAL, networkKind)

        return Wallet(
            descriptor = external,
            changeDescriptor = internal,
            network = network.toBdkNetwork(),
            persister = Persister.newInMemory(),
        )
    }

    private fun parseMnemonic(words: List<String>): Mnemonic {
        val normalized = words.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (normalized.size != 12 && normalized.size != 24) {
            throw InvalidBitcoinMnemonicException(
                "BIP-39 mnemonic must be 12 or 24 words, found ${normalized.size}",
            )
        }
        return try {
            // BIP-39: rejects unknown words and a bad checksum.
            Mnemonic.fromString(normalized.joinToString(" "))
        } catch (e: Exception) {
            throw InvalidBitcoinMnemonicException(
                formatBip39ExceptionMessage(e),
                e,
            )
        }
    }

    private fun BitcoinNetwork.toNetworkKind(): NetworkKind = when (this) {
        BitcoinNetwork.MAINNET -> NetworkKind.MAIN
        BitcoinNetwork.TESTNET4 -> NetworkKind.TEST
    }

    private fun BitcoinNetwork.toBdkNetwork(): Network = when (this) {
        BitcoinNetwork.MAINNET -> Network.BITCOIN
        BitcoinNetwork.TESTNET4 -> Network.TESTNET4
    }

    private companion object {
        const val RECEIVE_INDEX = 0u
        const val BELOW_DUST =
            "Amount is below the dust limit. Send at least 294 satoshis (0.00000294 BTC)."
    }
}

private fun String.decodeHex(): ByteArray {
    val hex = trim()
    require(hex.length % 2 == 0 && hex.isNotEmpty()) { "invalid hex" }
    return ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }

internal fun formatBip39ExceptionMessage(error: Throwable): String {
    val unknownIndex = unknownWordIndex(error)
    if (unknownIndex != null) {
        return "Unknown recovery word at position ${unknownIndex + 1}"
    }
    if (isChecksumError(error)) {
        return "Recovery phrase checksum is invalid"
    }
    return error.message?.takeIf { it.isNotBlank() } ?: "Invalid BIP-39 mnemonic"
}

private fun isChecksumError(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.any { throwable ->
        throwable.message.orEmpty().contains("checksum", ignoreCase = true) ||
            throwable.javaClass.simpleName.contains("InvalidChecksum", ignoreCase = true)
    }

private fun unknownWordIndex(error: Throwable): Int? {
    val fromMessage = Regex("""index[=:]?\s*(\d+)""")
        .find(error.message.orEmpty())
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()
    if (fromMessage != null) return fromMessage
    return runCatching {
        val index = error.javaClass.getDeclaredField("index").apply { isAccessible = true }
            .get(error)
        when (index) {
            null -> null
            is ULong -> index.toInt()
            is Long -> index.toInt()
            is Int -> index
            else -> null
        }
    }.getOrNull()
}
