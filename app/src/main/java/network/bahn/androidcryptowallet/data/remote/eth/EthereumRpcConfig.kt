package network.bahn.androidcryptowallet.data.remote.eth

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork

data class EthereumRpcConfig(
    val sepoliaRpcUrl: String,
    val mainnetRpcUrl: String,
) {
    fun rpcUrl(network: EthereumNetwork): String = when (network) {
        EthereumNetwork.SEPOLIA -> sepoliaRpcUrl
        EthereumNetwork.MAINNET -> mainnetRpcUrl
    }
}
