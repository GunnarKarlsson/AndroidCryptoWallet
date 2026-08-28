package network.bahn.androidcryptowallet.data.remote.eth

import network.bahn.androidcryptowallet.domain.model.EvmNetwork

data class EthereumRpcConfig(
    val sepoliaRpcUrl: String,
    val mainnetRpcUrl: String,
) {
    fun rpcUrl(network: EvmNetwork): String = when (network) {
        EvmNetwork.SEPOLIA -> sepoliaRpcUrl
        EvmNetwork.MAINNET -> mainnetRpcUrl
    }
}
