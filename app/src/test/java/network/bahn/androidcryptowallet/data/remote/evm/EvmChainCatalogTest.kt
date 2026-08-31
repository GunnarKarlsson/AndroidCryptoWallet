package network.bahn.androidcryptowallet.data.remote.evm

import network.bahn.androidcryptowallet.data.repository.DefaultProviderCatalog
import network.bahn.androidcryptowallet.data.repository.ProviderIds
import network.bahn.androidcryptowallet.data.repository.testEvmChainCatalog
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import org.junit.Assert.assertEquals
import org.junit.Test

class EvmChainCatalogTest {
    private val defaults = DefaultProviderCatalog()
    private val catalog = testEvmChainCatalog()

    @Test
    fun rpcUrl_returnsDefaultUrls() {
        DefaultProviderCatalog.defaultRpcUrls.forEach { (network, url) ->
            assertEquals(url, catalog.rpcUrl(network))
        }
    }

    @Test
    fun rpcUrl_returnsOverrideWhenSet() {
        val customCatalog = testEvmChainCatalog(
            overrides = mapOf(ProviderIds.evmRpc(EvmNetwork.SEPOLIA) to "https://custom-rpc.example"),
        )
        assertEquals("https://custom-rpc.example", customCatalog.rpcUrl(EvmNetwork.SEPOLIA))
    }

    @Test
    fun explorerBaseUrl_returnsDefaultUrls() {
        DefaultProviderCatalog.defaultExplorerEndpoints.forEach { (network, endpoint) ->
            assertEquals(endpoint.baseUrl, catalog.explorerBaseUrl(network))
        }
    }

    @Test
    fun explorerKind_returnsConfiguredKinds() {
        assertEquals(
            defaults.explorerKind(EvmNetwork.SEPOLIA),
            catalog.explorerKind(EvmNetwork.SEPOLIA),
        )
        assertEquals(
            defaults.explorerKind(EvmNetwork.BSC_MAINNET),
            catalog.explorerKind(EvmNetwork.BSC_MAINNET),
        )
    }

    @Test
    fun rpcUrl_returnsConfiguredUrlsFromNewChainsDoc() {
        assertEquals(
            DefaultProviderCatalog.defaultRpcUrls.getValue(EvmNetwork.ARBITRUM_SEPOLIA),
            catalog.rpcUrl(EvmNetwork.ARBITRUM_SEPOLIA),
        )
        assertEquals(
            DefaultProviderCatalog.defaultRpcUrls.getValue(EvmNetwork.AVALANCHE_MAINNET),
            catalog.rpcUrl(EvmNetwork.AVALANCHE_MAINNET),
        )
    }
}
