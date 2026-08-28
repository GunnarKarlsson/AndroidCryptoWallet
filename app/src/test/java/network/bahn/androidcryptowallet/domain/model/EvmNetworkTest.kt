package network.bahn.androidcryptowallet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvmNetworkTest {
    @Test
    fun networksForEthereum_returnsOnlyEthereumNetworks() {
        val networks = EvmNetwork.networksFor(EvmFamily.ETHEREUM)
        assertEquals(listOf(EvmNetwork.SEPOLIA, EvmNetwork.MAINNET), networks)
    }

    @Test
    fun networksForBsc_isEmptyUntilBscNetworksAreAdded() {
        assertTrue(EvmNetwork.networksFor(EvmFamily.BSC).isEmpty())
    }

    @Test
    fun sepoliaAndMainnetKeepStoredNamesAndEthSymbol() {
        assertEquals("SEPOLIA", EvmNetwork.SEPOLIA.name)
        assertEquals("MAINNET", EvmNetwork.MAINNET.name)
        assertEquals(11_155_111L, EvmNetwork.SEPOLIA.chainId)
        assertEquals(1L, EvmNetwork.MAINNET.chainId)
        assertEquals("ETH", EvmNetwork.SEPOLIA.nativeSymbol)
        assertEquals("ETH", EvmNetwork.MAINNET.nativeSymbol)
        assertEquals(EvmFamily.ETHEREUM, EvmNetwork.SEPOLIA.family)
        assertEquals(EvmFamily.ETHEREUM, EvmNetwork.MAINNET.family)
    }
}
