package network.bahn.androidcryptowallet.data.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinScriptType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BIP-84 test vector (account 0, first receive address) from
 * https://github.com/bitcoin/bips/blob/master/bip-0084.mediawiki
 */
@RunWith(AndroidJUnit4::class)
class BdkBitcoinKeyEngineInstrumentedTest {
    @Test
    fun bip84MainnetReceiveAddressIndex0() {
        val engine = BdkBitcoinKeyEngine()
        val addresses = engine.deriveReceiveAddresses(BIP84_MNEMONIC_WORDS, passphrase = null)
        val mainnet = addresses.first { it.network == BitcoinNetwork.MAINNET }

        assertEquals(BitcoinScriptType.BIP84, mainnet.scriptType)
        assertEquals(0, mainnet.index)
        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", mainnet.address)
    }

    private companion object {
        val BIP84_MNEMONIC_WORDS = listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "about",
        )
    }
}
