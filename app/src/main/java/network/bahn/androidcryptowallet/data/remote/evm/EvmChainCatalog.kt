package network.bahn.androidcryptowallet.data.remote.evm

import network.bahn.androidcryptowallet.data.repository.DefaultProviderCatalog
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.repository.ProviderSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvmChainCatalog @Inject constructor(
    private val defaultProviderCatalog: DefaultProviderCatalog,
    private val providerSettingsRepository: ProviderSettingsRepository,
) {
    fun rpcUrl(network: EvmNetwork): String =
        providerSettingsRepository.resolveUrl(
            defaultProviderCatalog.evmRpcProviderId(network),
        )

    fun explorerEndpoint(network: EvmNetwork): EvmExplorerEndpoint {
        val defaults = defaultProviderCatalog.defaultExplorerEndpoint(network)
        return defaults.copy(
            baseUrl = providerSettingsRepository.resolveUrl(
                defaultProviderCatalog.evmExplorerProviderId(network),
            ),
        )
    }

    fun explorerBaseUrl(network: EvmNetwork): String =
        explorerEndpoint(network).baseUrl

    fun explorerKind(network: EvmNetwork): EvmExplorerKind =
        defaultProviderCatalog.explorerKind(network)
}
