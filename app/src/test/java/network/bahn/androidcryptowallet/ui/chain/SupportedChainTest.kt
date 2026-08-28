package network.bahn.androidcryptowallet.ui.chain

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportedChainTest {
    @Test
    fun hardcodedListIncludesAllSupportedChains() {
        assertEquals(
            listOf(
                SupportedChain.BITCOIN,
                SupportedChain.ETHEREUM,
                SupportedChain.BSC,
                SupportedChain.POLYGON,
                SupportedChain.ARBITRUM,
                SupportedChain.BASE,
                SupportedChain.OPTIMISM,
                SupportedChain.AVALANCHE,
            ),
            SupportedChains,
        )
    }
}
