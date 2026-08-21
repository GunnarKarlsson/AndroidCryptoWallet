package network.bahn.androidcryptowallet.data.remote.ms

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

data class MsBitcoinConfig(
    val testnet4BaseUrl: String,
    val mainnetBaseUrl: String,
) {
    fun baseUrl(network: BitcoinNetwork): String = when (network) {
        BitcoinNetwork.TESTNET4 -> testnet4BaseUrl
        BitcoinNetwork.MAINNET -> mainnetBaseUrl
    }

    fun heightUrl(network: BitcoinNetwork): String {
        val base = baseUrl(network).let { url ->
            if (url.endsWith("/")) url else "$url/"
        }
        val path = when (network) {
            BitcoinNetwork.TESTNET4 -> TESTNET4_HEIGHT_PATH
            BitcoinNetwork.MAINNET -> MAINNET_HEIGHT_PATH
        }
        return base + path
    }

    private companion object {
        const val TESTNET4_HEIGHT_PATH = "blocks/tip/height"
        const val MAINNET_HEIGHT_PATH = "v1/blocks/tip/height"
    }
}
