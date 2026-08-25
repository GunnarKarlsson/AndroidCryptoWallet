package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.domain.model.InvalidEthereumMnemonicException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
            error("expected InvalidEthereumMnemonicException")
        } catch (e: InvalidEthereumMnemonicException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }

    private companion object {
        val ABANDON_WORDS = List(11) { "abandon" } + "about"
        const val ABANDON_ADDRESS = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94"
    }
}
