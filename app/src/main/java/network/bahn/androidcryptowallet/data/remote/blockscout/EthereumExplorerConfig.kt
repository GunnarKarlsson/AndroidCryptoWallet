package network.bahn.androidcryptowallet.data.remote.blockscout

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork

data class EthereumExplorerConfig(
    val sepoliaBaseUrl: String,
    val mainnetBaseUrl: String,
) {
    fun baseUrl(network: EthereumNetwork): String = when (network) {
        EthereumNetwork.SEPOLIA -> sepoliaBaseUrl
        EthereumNetwork.MAINNET -> mainnetBaseUrl
    }
}
