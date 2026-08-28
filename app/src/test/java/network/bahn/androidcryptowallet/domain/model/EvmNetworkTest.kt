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
    fun networksForPolygon_returnsOnlyPolygonNetworks() {
        val networks = EvmNetwork.networksFor(EvmFamily.POLYGON)
        assertEquals(listOf(EvmNetwork.POLYGON_AMOY, EvmNetwork.POLYGON_MAINNET), networks)
    }

    @Test
    fun polygonNetworksHaveExpectedChainIdsAndSymbols() {
        assertEquals(80_002L, EvmNetwork.POLYGON_AMOY.chainId)
        assertEquals(137L, EvmNetwork.POLYGON_MAINNET.chainId)
        assertEquals("POL", EvmNetwork.POLYGON_AMOY.nativeSymbol)
        assertEquals("POL", EvmNetwork.POLYGON_MAINNET.nativeSymbol)
        assertEquals(EvmFamily.POLYGON, EvmNetwork.POLYGON_AMOY.family)
        assertEquals(EvmFamily.POLYGON, EvmNetwork.POLYGON_MAINNET.family)
    }

    @Test
    fun networksForArbitrum_returnsOnlyArbitrumNetworks() {
        val networks = EvmNetwork.networksFor(EvmFamily.ARBITRUM)
        assertEquals(listOf(EvmNetwork.ARBITRUM_SEPOLIA, EvmNetwork.ARBITRUM_MAINNET), networks)
    }

    @Test
    fun arbitrumNetworksHaveExpectedChainIdsAndSymbols() {
        assertEquals(421_614L, EvmNetwork.ARBITRUM_SEPOLIA.chainId)
        assertEquals(42_161L, EvmNetwork.ARBITRUM_MAINNET.chainId)
        assertEquals("ETH", EvmNetwork.ARBITRUM_SEPOLIA.nativeSymbol)
        assertEquals("ETH", EvmNetwork.ARBITRUM_MAINNET.nativeSymbol)
    }

    @Test
    fun networksForBase_returnsOnlyBaseNetworks() {
        val networks = EvmNetwork.networksFor(EvmFamily.BASE)
        assertEquals(listOf(EvmNetwork.BASE_SEPOLIA, EvmNetwork.BASE_MAINNET), networks)
    }

    @Test
    fun baseNetworksHaveExpectedChainIdsAndSymbols() {
        assertEquals(84_532L, EvmNetwork.BASE_SEPOLIA.chainId)
        assertEquals(8453L, EvmNetwork.BASE_MAINNET.chainId)
        assertEquals("ETH", EvmNetwork.BASE_SEPOLIA.nativeSymbol)
        assertEquals("ETH", EvmNetwork.BASE_MAINNET.nativeSymbol)
    }

    @Test
    fun networksForOptimism_returnsOnlyOptimismNetworks() {
        val networks = EvmNetwork.networksFor(EvmFamily.OPTIMISM)
        assertEquals(listOf(EvmNetwork.OPTIMISM_SEPOLIA, EvmNetwork.OPTIMISM_MAINNET), networks)
    }

    @Test
    fun optimismNetworksHaveExpectedChainIdsAndSymbols() {
        assertEquals(11_155_420L, EvmNetwork.OPTIMISM_SEPOLIA.chainId)
        assertEquals(10L, EvmNetwork.OPTIMISM_MAINNET.chainId)
        assertEquals("ETH", EvmNetwork.OPTIMISM_SEPOLIA.nativeSymbol)
        assertEquals("ETH", EvmNetwork.OPTIMISM_MAINNET.nativeSymbol)
    }

    @Test
    fun networksForAvalanche_returnsOnlyAvalancheNetworks() {
        val networks = EvmNetwork.networksFor(EvmFamily.AVALANCHE)
        assertEquals(listOf(EvmNetwork.AVALANCHE_FUJI, EvmNetwork.AVALANCHE_MAINNET), networks)
    }

    @Test
    fun avalancheNetworksHaveExpectedChainIdsAndSymbols() {
        assertEquals(43_113L, EvmNetwork.AVALANCHE_FUJI.chainId)
        assertEquals(43_114L, EvmNetwork.AVALANCHE_MAINNET.chainId)
        assertEquals("AVAX", EvmNetwork.AVALANCHE_FUJI.nativeSymbol)
        assertEquals("AVAX", EvmNetwork.AVALANCHE_MAINNET.nativeSymbol)
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
