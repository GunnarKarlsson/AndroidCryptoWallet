package network.bahn.androidcryptowallet.ui.chain

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportedChainTest {
    @Test
    fun hardcodedListIsBitcoinThenEthereum() {
        assertEquals(
            listOf(SupportedChain.BITCOIN, SupportedChain.ETHEREUM),
            SupportedChains,
        )
    }
}
