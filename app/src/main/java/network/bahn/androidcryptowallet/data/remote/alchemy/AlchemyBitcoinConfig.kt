package network.bahn.androidcryptowallet.data.remote.alchemy

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

data class AlchemyBitcoinConfig(
    val apiKey: String,
    val testnet4BaseUrl: String,
    val mainnetBaseUrl: String,
) {
    fun baseUrl(network: BitcoinNetwork): String = when (network) {
        BitcoinNetwork.TESTNET4 -> testnet4BaseUrl
        BitcoinNetwork.MAINNET -> mainnetBaseUrl
    }
}
