package network.bahn.androidcryptowallet.data.remote.ms

import network.bahn.androidcryptowallet.data.repository.DefaultProviderCatalog
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.repository.ProviderSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MsBitcoinConfig @Inject constructor(
    private val defaultProviderCatalog: DefaultProviderCatalog,
    private val providerSettingsRepository: ProviderSettingsRepository,
) {
    fun baseUrl(network: BitcoinNetwork): String =
        providerSettingsRepository.resolveUrl(
            defaultProviderCatalog.bitcoinProviderId(network),
        )

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
