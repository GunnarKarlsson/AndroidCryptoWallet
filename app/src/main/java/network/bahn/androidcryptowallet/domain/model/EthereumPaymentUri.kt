package network.bahn.androidcryptowallet.domain.model

/**
 * EIP-681 payment URI with chain id so scanners know Sepolia vs Mainnet.
 * Amount and function data are omitted.
 */
object EthereumPaymentUri {
    fun fromAddress(address: String, network: EvmNetwork): String =
        "ethereum:$address@${network.chainId}"
}
