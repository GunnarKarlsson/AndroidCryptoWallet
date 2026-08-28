package network.bahn.androidcryptowallet.data.remote.evm

import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import org.junit.Assert.assertEquals
import org.junit.Test

class EvmChainCatalogTest {
    private val catalog = EvmChainCatalog(
        rpcUrls = mapOf(
            EvmNetwork.SEPOLIA to "https://ethereum-sepolia-rpc.publicnode.com",
            EvmNetwork.MAINNET to "https://ethereum.publicnode.com",
        ),
        explorerBaseUrls = mapOf(
            EvmNetwork.SEPOLIA to "https://eth-sepolia.blockscout.com/api/v2",
            EvmNetwork.MAINNET to "https://eth.blockscout.com/api/v2",
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
}
