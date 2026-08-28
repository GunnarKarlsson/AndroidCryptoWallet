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
        ),
        explorerBaseUrls = mapOf(
            EvmNetwork.SEPOLIA to "https://eth-sepolia.blockscout.com/api/v2",
            EvmNetwork.MAINNET to "https://eth.blockscout.com/api/v2",
            EvmNetwork.BSC_TESTNET to "https://api-testnet.bscscan.com/api",
            EvmNetwork.BSC_MAINNET to "https://api.bscscan.com/api",
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
}
