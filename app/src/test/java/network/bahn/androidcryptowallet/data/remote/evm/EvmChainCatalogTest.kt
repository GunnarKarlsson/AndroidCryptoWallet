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
}
