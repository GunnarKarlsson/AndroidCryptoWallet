package network.bahn.androidcryptowallet.data.remote.evm

import network.bahn.androidcryptowallet.domain.model.EvmNetwork

data class EvmChainCatalog(
    private val rpcUrls: Map<EvmNetwork, String>,
    private val explorerEndpoints: Map<EvmNetwork, EvmExplorerEndpoint>,
) {
    fun rpcUrl(network: EvmNetwork): String =
        rpcUrls[network] ?: error("No RPC URL configured for $network")

    fun explorerEndpoint(network: EvmNetwork): EvmExplorerEndpoint =
        explorerEndpoints[network] ?: error("No explorer configured for $network")

    fun explorerBaseUrl(network: EvmNetwork): String =
        explorerEndpoint(network).baseUrl

    fun explorerKind(network: EvmNetwork): EvmExplorerKind =
        explorerEndpoint(network).kind
}
