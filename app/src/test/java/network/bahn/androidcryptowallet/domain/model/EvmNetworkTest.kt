package network.bahn.androidcryptowallet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EvmNetworkTest {
    @Test
    fun networksForEthereum_returnsOnlyEthereumNetworks() {
        val networks = EvmNetwork.networksFor(EvmFamily.ETHEREUM)
        assertEquals(listOf(EvmNetwork.SEPOLIA, EvmNetwork.MAINNET), networks)
    }

    @Test
    fun networksForBsc_returnsOnlyBscNetworks() {
        val networks = EvmNetwork.networksFor(EvmFamily.BSC)
        assertEquals(listOf(EvmNetwork.BSC_TESTNET, EvmNetwork.BSC_MAINNET), networks)
    }

    @Test
    fun bscNetworksHaveExpectedChainIdsAndSymbols() {
        assertEquals(97L, EvmNetwork.BSC_TESTNET.chainId)
        assertEquals(56L, EvmNetwork.BSC_MAINNET.chainId)
        assertEquals("tBNB", EvmNetwork.BSC_TESTNET.nativeSymbol)
        assertEquals("BNB", EvmNetwork.BSC_MAINNET.nativeSymbol)
        assertEquals(EvmFamily.BSC, EvmNetwork.BSC_TESTNET.family)
        assertEquals(EvmFamily.BSC, EvmNetwork.BSC_MAINNET.family)
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
