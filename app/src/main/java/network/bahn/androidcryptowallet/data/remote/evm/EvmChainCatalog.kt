package network.bahn.androidcryptowallet.data.remote.evm

import network.bahn.androidcryptowallet.domain.model.EvmNetwork

data class EvmChainCatalog(
    private val rpcUrls: Map<EvmNetwork, String>,
    private val explorerBaseUrls: Map<EvmNetwork, String>,
) {
    fun rpcUrl(network: EvmNetwork): String =
        rpcUrls[network] ?: error("No RPC URL configured for $network")

    fun explorerBaseUrl(network: EvmNetwork): String =
        explorerBaseUrls[network] ?: error("No explorer base URL configured for $network")
}
