package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.InvalidBitcoinMnemonicException
import org.bitcoindevkit.Descriptor
import org.bitcoindevkit.DescriptorSecretKey
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Mnemonic
import org.bitcoindevkit.Network
import org.bitcoindevkit.NetworkKind
import org.bitcoindevkit.Persister
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
        return deriveReceiveAddress(mnemonic, password, network)
    }

    private fun deriveReceiveAddress(
        mnemonic: Mnemonic,
        password: String?,
        network: BitcoinNetwork,
    ): BitcoinReceiveAddress {
        // BIP-32 extended keys: NetworkKind is MAIN vs TEST (tprv/xprv). Testnet4
        // shares TEST keys with other test networks; Network.TESTNET4 is only for
        // address encoding / genesis (tb1q).
        val networkKind = network.toNetworkKind()
        val secretKey = DescriptorSecretKey(networkKind, mnemonic, password)

        // BIP-84: wpkh(key / 84' / {0,1}' / 0' / {0,1} / *). EXTERNAL = receive /0/*.
        val external = Descriptor.newBip84(secretKey, KeychainKind.EXTERNAL, networkKind)
        // BIP-84 change chain /1/* — built so later send can reuse this engine; not shown in UI.
        val internal = Descriptor.newBip84(secretKey, KeychainKind.INTERNAL, networkKind)

        val persister = Persister.newInMemory()
        val wallet = Wallet(
            descriptor = external,
            changeDescriptor = internal,
            network = network.toBdkNetwork(),
            persister = persister,
        )
        val addressInfo = wallet.peekAddress(KeychainKind.EXTERNAL, RECEIVE_INDEX)
        return BitcoinReceiveAddress(
            network = network,
            address = addressInfo.address.toString(),
            index = RECEIVE_INDEX.toInt(),
            scriptType = BitcoinScriptType.BIP84,
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
                e.message?.takeIf { it.isNotBlank() } ?: "Invalid BIP-39 mnemonic",
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
    }
}
