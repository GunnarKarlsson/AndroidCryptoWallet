package network.bahn.androidcryptowallet.data.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import network.bahn.androidcryptowallet.domain.model.InvalidBitcoinMnemonicException
import org.bitcoindevkit.Address
import org.bitcoindevkit.Network
import org.bitcoindevkit.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * BIP-84 test vector (account 0, first receive address) from
 * https://github.com/bitcoin/bips/blob/master/bip-0084.mediawiki
 */
@RunWith(AndroidJUnit4::class)
class BdkBitcoinKeyEngineInstrumentedTest {
    @Test
    fun bip84MainnetReceiveAddressIndex0() {
        val engine = BdkBitcoinKeyEngine()
        val mainnet = engine.deriveReceiveAddress(
            mnemonicWords = BIP84_MNEMONIC_WORDS,
            passphrase = null,
            network = BitcoinNetwork.MAINNET,
        )

        assertEquals(BitcoinScriptType.BIP84, mainnet.scriptType)
        assertEquals(0, mainnet.index)
        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", mainnet.address)
    }

    @Test
    fun generatedMnemonicRoundTripsThroughRestoreDerive() {
        val engine = BdkBitcoinKeyEngine()
        val words = engine.generateMnemonic()
        assertEquals(12, words.size)

        engine.validateMnemonic(words)
        val created = engine.deriveReceiveAddress(
            mnemonicWords = words,
            passphrase = null,
            network = BitcoinNetwork.TESTNET4,
        )
        engine.validateMnemonic(words)
        val restored = engine.deriveReceiveAddress(
            mnemonicWords = words,
            passphrase = null,
            network = BitcoinNetwork.TESTNET4,
        )

        assertEquals(created.address, restored.address)
        assertEquals(BitcoinScriptType.BIP84, restored.scriptType)
        assertEquals(0, restored.index)
    }

    @Test
    fun sameMnemonicAndPassphraseRestoresSameAddress() {
        val engine = BdkBitcoinKeyEngine()
        val words = engine.generateMnemonic()
        val created = engine.deriveReceiveAddress(
            mnemonicWords = words,
            passphrase = "tulip",
            network = BitcoinNetwork.TESTNET4,
        )
        val restored = engine.deriveReceiveAddress(
            mnemonicWords = words,
            passphrase = "tulip",
            network = BitcoinNetwork.TESTNET4,
        )
        val withoutPassphrase = engine.deriveReceiveAddress(
            mnemonicWords = words,
            passphrase = null,
            network = BitcoinNetwork.TESTNET4,
        )

        assertEquals(created.address, restored.address)
        assertNotEquals(created.address, withoutPassphrase.address)
    }

    @Test
    fun bip84MnemonicRestoresPublishedReceiveAddress() {
        val engine = BdkBitcoinKeyEngine()
        engine.validateMnemonic(BIP84_MNEMONIC_WORDS)
        val restored = engine.deriveReceiveAddress(
            mnemonicWords = BIP84_MNEMONIC_WORDS,
            passphrase = null,
            network = BitcoinNetwork.MAINNET,
        )

        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", restored.address)
    }

    @Test
    fun unknownWordIsRejectedWithPosition() {
        val engine = BdkBitcoinKeyEngine()
        val words = listOf("cold") + BIP84_MNEMONIC_WORDS.drop(1)
        try {
            engine.validateMnemonic(words)
            fail("expected InvalidBitcoinMnemonicException")
        } catch (e: InvalidBitcoinMnemonicException) {
            assertEquals("Unknown recovery word at position 1", e.message)
        }
    }

    @Test
    fun invalidChecksumIsRejected() {
        val engine = BdkBitcoinKeyEngine()
        val words = List(12) { "abandon" }
        try {
            engine.validateMnemonic(words)
            fail("expected InvalidBitcoinMnemonicException")
        } catch (e: InvalidBitcoinMnemonicException) {
            assertEquals("Recovery phrase checksum is invalid", e.message)
        }
    }

    @Test
    fun isValidAddressAcceptsMatchingNetwork() {
        val engine = BdkBitcoinKeyEngine()
        val mainnet = engine.deriveReceiveAddress(
            mnemonicWords = BIP84_MNEMONIC_WORDS,
            passphrase = null,
            network = BitcoinNetwork.MAINNET,
        )
        val testnet4 = engine.deriveReceiveAddress(
            mnemonicWords = BIP84_MNEMONIC_WORDS,
            passphrase = null,
            network = BitcoinNetwork.TESTNET4,
        )
        assertTrue(engine.isValidAddress(BitcoinNetwork.MAINNET, mainnet.address))
        assertTrue(engine.isValidAddress(BitcoinNetwork.TESTNET4, testnet4.address))
    }

    @Test
    fun isValidAddressRejectsWrongNetworkAndGarbage() {
        val engine = BdkBitcoinKeyEngine()
        assertFalse(
            engine.isValidAddress(
                BitcoinNetwork.MAINNET,
                "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34",
            ),
        )
        assertFalse(engine.isValidAddress(BitcoinNetwork.MAINNET, "not-an-address"))
        assertFalse(engine.isValidAddress(BitcoinNetwork.TESTNET4, ""))
    }

    @Test
    fun buildAndSignSendProducesParseableHex() {
        val engine = BdkBitcoinKeyEngine()
        val receive = engine.deriveReceiveAddress(
            mnemonicWords = BIP84_MNEMONIC_WORDS,
            passphrase = null,
            network = BitcoinNetwork.MAINNET,
        )
        val fundingHex = fundingTxHexPayingTo(receive.address, valueSats = 100_000L)

        val signed = engine.buildAndSignSend(
            mnemonicWords = BIP84_MNEMONIC_WORDS,
            passphrase = null,
            network = BitcoinNetwork.MAINNET,
            fundingTxHexes = listOf(fundingHex),
            recipientAddress = BIP173_EXAMPLE,
            amountSatoshis = 20_000L,
            feeRateSatPerVbyte = 5L,
            changeAddress = receive.address,
        )

        assertTrue(signed.rawHex.isNotEmpty())
        assertEquals(signed.txid, Transaction(signed.rawHex.decodeHex()).computeTxid().toString())
    }

    private companion object {
        val BIP84_MNEMONIC_WORDS = listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "about",
        )
        const val BIP173_EXAMPLE = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
    }
}

private fun fundingTxHexPayingTo(address: String, valueSats: Long): String {
    val script = Address(address, Network.BITCOIN).scriptPubkey().toBytes()
    val out = ByteArrayOutputStream()
    writeInt32Le(out, 2)
    out.write(1)
    repeat(32) { out.write(0x11) }
    writeInt32Le(out, 0)
    out.write(0)
    writeInt32Le(out, -1)
    out.write(1)
    writeInt64Le(out, valueSats)
    out.write(script.size)
    out.write(script)
    writeInt32Le(out, 0)
    return out.toByteArray().toHex()
}

private fun writeInt32Le(out: ByteArrayOutputStream, value: Int) {
    out.write(value and 0xFF)
    out.write(value ushr 8 and 0xFF)
    out.write(value ushr 16 and 0xFF)
    out.write(value ushr 24 and 0xFF)
}

private fun writeInt64Le(out: ByteArrayOutputStream, value: Long) {
    var remaining = value
    repeat(8) {
        out.write((remaining and 0xFF).toInt())
        remaining = remaining ushr 8
    }
}

private fun ByteArray.toHex(): String =
    joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

private fun String.decodeHex(): ByteArray {
    val hex = trim()
    return ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
