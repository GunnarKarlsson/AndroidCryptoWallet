package network.bahn.androidcryptowallet.data.remote.blockscout

import network.bahn.androidcryptowallet.domain.model.EvmNetwork

data class EthereumExplorerConfig(
    val sepoliaBaseUrl: String,
    val mainnetBaseUrl: String,
) {
    fun baseUrl(network: EvmNetwork): String = when (network) {
        EvmNetwork.SEPOLIA -> sepoliaBaseUrl
        EvmNetwork.MAINNET -> mainnetBaseUrl
    }
}
