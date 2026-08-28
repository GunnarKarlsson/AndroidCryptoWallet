package network.bahn.androidcryptowallet.data.remote.evm

import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import org.junit.Assert.assertEquals
import org.junit.Test

class EvmChainCatalogTest {
    private val catalog = EvmChainCatalog(
        rpcUrls = mapOf(
            EvmNetwork.SEPOLIA to "https://ethereum-sepolia-rpc.publicnode.com",
            EvmNetwork.MAINNET to "https://ethereum.publicnode.com",
            EvmNetwork.BSC_TESTNET to "https://data-seed-prebsc-1-s1.bnbchain.org:8545",
            EvmNetwork.BSC_MAINNET to "https://bsc-dataseed.bnbchain.org",
            EvmNetwork.POLYGON_AMOY to "https://polygon-amoy-bor-rpc.publicnode.com",
            EvmNetwork.POLYGON_MAINNET to "https://polygon-bor-rpc.publicnode.com",
            EvmNetwork.ARBITRUM_SEPOLIA to "https://sepolia-rollup.arbitrum.io/rpc",
            EvmNetwork.ARBITRUM_MAINNET to "https://arb1.arbitrum.io/rpc",
            EvmNetwork.BASE_SEPOLIA to "https://sepolia.base.org",
            EvmNetwork.BASE_MAINNET to "https://mainnet.base.org",
            EvmNetwork.OPTIMISM_SEPOLIA to "https://sepolia.optimism.io",
            EvmNetwork.OPTIMISM_MAINNET to "https://mainnet.optimism.io",
            EvmNetwork.AVALANCHE_FUJI to "https://api.avax-test.network/ext/bc/C/rpc",
            EvmNetwork.AVALANCHE_MAINNET to "https://api.avax.network/ext/bc/C/rpc",
        ),
        explorerEndpoints = mapOf(
            EvmNetwork.SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://eth-sepolia.blockscout.com/api/v2",
                kind = EvmExplorerKind.BLOCKSCOUT,
            ),
            EvmNetwork.MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://eth.blockscout.com/api/v2",
                kind = EvmExplorerKind.BLOCKSCOUT,
            ),
            EvmNetwork.BSC_TESTNET to EvmExplorerEndpoint(
                baseUrl = "https://api-testnet.bscscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BSC_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.bscscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.POLYGON_AMOY to EvmExplorerEndpoint(
                baseUrl = "https://api-amoy.polygonscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.POLYGON_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.polygonscan.com/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.ARBITRUM_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia.arbiscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.ARBITRUM_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.arbiscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BASE_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia.basescan.org/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.BASE_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.basescan.org/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.OPTIMISM_SEPOLIA to EvmExplorerEndpoint(
                baseUrl = "https://api-sepolia-optimistic.etherscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.OPTIMISM_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api-optimistic.etherscan.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.AVALANCHE_FUJI to EvmExplorerEndpoint(
                baseUrl = "https://api-testnet.snowtrace.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
            EvmNetwork.AVALANCHE_MAINNET to EvmExplorerEndpoint(
                baseUrl = "https://api.snowtrace.io/api",
                kind = EvmExplorerKind.ETHERSCAN,
            ),
        ),
    )

    @Test
    fun rpcUrl_returnsConfiguredUrlForSepolia() {
        assertEquals(
            "https://ethereum-sepolia-rpc.publicnode.com",
            catalog.rpcUrl(EvmNetwork.SEPOLIA),
        )
    }

    @Test
    fun rpcUrl_returnsConfiguredUrlForMainnet() {
        assertEquals(
            "https://ethereum.publicnode.com",
            catalog.rpcUrl(EvmNetwork.MAINNET),
        )
    }

    @Test
    fun explorerBaseUrl_returnsConfiguredUrlForSepolia() {
        assertEquals(
            "https://eth-sepolia.blockscout.com/api/v2",
            catalog.explorerBaseUrl(EvmNetwork.SEPOLIA),
        )
    }

    @Test
    fun explorerBaseUrl_returnsConfiguredUrlForMainnet() {
        assertEquals(
            "https://eth.blockscout.com/api/v2",
            catalog.explorerBaseUrl(EvmNetwork.MAINNET),
        )
    }

    @Test
    fun explorerKind_returnsBlockscoutForEthereumNetworks() {
        assertEquals(EvmExplorerKind.BLOCKSCOUT, catalog.explorerKind(EvmNetwork.SEPOLIA))
        assertEquals(EvmExplorerKind.BLOCKSCOUT, catalog.explorerKind(EvmNetwork.MAINNET))
    }

    @Test
    fun explorerKind_returnsEtherscanForBscNetworks() {
        assertEquals(EvmExplorerKind.ETHERSCAN, catalog.explorerKind(EvmNetwork.BSC_TESTNET))
        assertEquals(EvmExplorerKind.ETHERSCAN, catalog.explorerKind(EvmNetwork.BSC_MAINNET))
    }

    @Test
    fun rpcUrl_returnsConfiguredUrlForBscTestnet() {
        assertEquals(
            "https://data-seed-prebsc-1-s1.bnbchain.org:8545",
            catalog.rpcUrl(EvmNetwork.BSC_TESTNET),
        )
    }

    @Test
    fun rpcUrl_returnsConfiguredUrlForBscMainnet() {
        assertEquals(
            "https://bsc-dataseed.bnbchain.org",
            catalog.rpcUrl(EvmNetwork.BSC_MAINNET),
        )
    }

    @Test
    fun explorerKind_returnsEtherscanForPolygonNetworks() {
        assertEquals(EvmExplorerKind.ETHERSCAN, catalog.explorerKind(EvmNetwork.POLYGON_AMOY))
        assertEquals(EvmExplorerKind.ETHERSCAN, catalog.explorerKind(EvmNetwork.POLYGON_MAINNET))
    }

    @Test
    fun rpcUrl_returnsConfiguredUrlForPolygonAmoy() {
        assertEquals(
            "https://polygon-amoy-bor-rpc.publicnode.com",
            catalog.rpcUrl(EvmNetwork.POLYGON_AMOY),
        )
    }

    @Test
    fun rpcUrl_returnsConfiguredUrlForPolygonMainnet() {
        assertEquals(
            "https://polygon-bor-rpc.publicnode.com",
            catalog.rpcUrl(EvmNetwork.POLYGON_MAINNET),
        )
    }

    @Test
    fun explorerKind_returnsEtherscanForNewChainNetworks() {
        assertEquals(EvmExplorerKind.ETHERSCAN, catalog.explorerKind(EvmNetwork.ARBITRUM_SEPOLIA))
        assertEquals(EvmExplorerKind.ETHERSCAN, catalog.explorerKind(EvmNetwork.BASE_MAINNET))
        assertEquals(EvmExplorerKind.ETHERSCAN, catalog.explorerKind(EvmNetwork.OPTIMISM_SEPOLIA))
        assertEquals(EvmExplorerKind.ETHERSCAN, catalog.explorerKind(EvmNetwork.AVALANCHE_FUJI))
    }

    @Test
    fun rpcUrl_returnsConfiguredUrlsFromNewChainsDoc() {
        assertEquals("https://sepolia-rollup.arbitrum.io/rpc", catalog.rpcUrl(EvmNetwork.ARBITRUM_SEPOLIA))
        assertEquals("https://arb1.arbitrum.io/rpc", catalog.rpcUrl(EvmNetwork.ARBITRUM_MAINNET))
        assertEquals("https://sepolia.base.org", catalog.rpcUrl(EvmNetwork.BASE_SEPOLIA))
        assertEquals("https://mainnet.base.org", catalog.rpcUrl(EvmNetwork.BASE_MAINNET))
        assertEquals("https://sepolia.optimism.io", catalog.rpcUrl(EvmNetwork.OPTIMISM_SEPOLIA))
        assertEquals("https://mainnet.optimism.io", catalog.rpcUrl(EvmNetwork.OPTIMISM_MAINNET))
        assertEquals(
            "https://api.avax-test.network/ext/bc/C/rpc",
            catalog.rpcUrl(EvmNetwork.AVALANCHE_FUJI),
        )
        assertEquals(
            "https://api.avax.network/ext/bc/C/rpc",
            catalog.rpcUrl(EvmNetwork.AVALANCHE_MAINNET),
        )
    }
}
