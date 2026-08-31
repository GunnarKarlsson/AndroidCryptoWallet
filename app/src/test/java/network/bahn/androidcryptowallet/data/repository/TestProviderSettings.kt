package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import network.bahn.androidcryptowallet.data.remote.evm.EvmChainCatalog
import network.bahn.androidcryptowallet.data.remote.ms.MsBitcoinConfig
import network.bahn.androidcryptowallet.domain.model.ProviderSetting
import network.bahn.androidcryptowallet.domain.repository.ProviderSettingsRepository

/** Test double with fixed URL overrides layered on [DefaultProviderCatalog]. */
class StaticProviderSettingsRepository(
    private val overrides: Map<String, String> = emptyMap(),
    private val defaults: DefaultProviderCatalog = DefaultProviderCatalog(),
) : ProviderSettingsRepository {
    private val mutableOverrides = overrides.toMutableMap()

    override fun observeProviders(): Flow<List<ProviderSetting>> = flowOf(emptyList())

    override fun observeProvider(id: String): Flow<ProviderSetting?> = flowOf(null)

    override fun resolveUrl(providerId: String): String =
        mutableOverrides[providerId] ?: defaults.defaultUrl(providerId)

    override suspend fun setUrl(providerId: String, url: String) {
        mutableOverrides[providerId] = url.trim()
    }

    override suspend fun resetToDefault(providerId: String) {
        mutableOverrides.remove(providerId)
    }
}

fun testEvmChainCatalog(
    overrides: Map<String, String> = emptyMap(),
): EvmChainCatalog = EvmChainCatalog(
    defaultProviderCatalog = DefaultProviderCatalog(),
    providerSettingsRepository = StaticProviderSettingsRepository(overrides),
)

fun testMsBitcoinConfig(
    testnet4BaseUrl: String,
    mainnetBaseUrl: String,
): MsBitcoinConfig {
    val defaults = DefaultProviderCatalog()
    return MsBitcoinConfig(
        defaultProviderCatalog = defaults,
        providerSettingsRepository = StaticProviderSettingsRepository(
            mapOf(
                ProviderIds.BITCOIN_TESTNET4 to testnet4BaseUrl,
                ProviderIds.BITCOIN_MAINNET to mainnetBaseUrl,
            ),
            defaults,
        ),
    )
}
