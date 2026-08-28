package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.InvalidEvmMnemonicException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class Web3jEthereumKeyEngineTest {
    private val engine = Web3jEthereumKeyEngine()

    @Test
    fun abandonMnemonicDerivesPublishedFirstAccount() {
        val derived = engine.deriveReceiveAddress(ABANDON_WORDS, passphrase = null)
        assertEquals(ABANDON_ADDRESS, derived.address)
        assertEquals(0, derived.index)
    }

    @Test
    fun invalidMnemonicThrows() {
        try {
            engine.validateMnemonic(listOf("not", "a", "valid", "mnemonic"))
            error("expected InvalidEvmMnemonicException")
        } catch (e: InvalidEvmMnemonicException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }

    @Test
    fun isValidAddressAcceptsChecksummedAndLowercase() {
        assertTrue(engine.isValidAddress(ABANDON_ADDRESS))
        assertTrue(engine.isValidAddress(ABANDON_ADDRESS.lowercase()))
        assertFalse(engine.isValidAddress("0x1234"))
        assertFalse(engine.isValidAddress("not-an-address"))
    }

    @Test
    fun buildAndSignSendProducesRawHex() {
        val signed = engine.buildAndSignSend(
            mnemonicWords = ABANDON_WORDS,
            passphrase = null,
            chainId = 11_155_111L,
            to = "0x2222222222222222222222222222222222222222",
            valueWei = BigInteger("1000000000000000"),
            nonce = 0L,
            gasLimit = 21_000L,
            maxPriorityFeePerGasWei = BigInteger("1500000000"),
            maxFeePerGasWei = BigInteger("3500000000"),
        )
        assertTrue(signed.startsWith("0x"))
        assertTrue(signed.length > 10)
    }

    private companion object {
        val ABANDON_WORDS = List(11) { "abandon" } + "about"
        const val ABANDON_ADDRESS = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94"
    }
}
