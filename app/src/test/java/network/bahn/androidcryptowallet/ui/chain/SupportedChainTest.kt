package network.bahn.androidcryptowallet.ui.chain

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportedChainTest {
    @Test
    fun hardcodedListIsBitcoinThenEthereumThenBsc() {
        assertEquals(
            listOf(SupportedChain.BITCOIN, SupportedChain.ETHEREUM, SupportedChain.BSC),
            SupportedChains,
        )
    }
}
