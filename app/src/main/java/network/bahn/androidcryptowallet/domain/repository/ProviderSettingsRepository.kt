package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.ProviderSetting

interface ProviderSettingsRepository {
    fun observeProviders(): Flow<List<ProviderSetting>>

    fun observeProvider(id: String): Flow<ProviderSetting?>

    /** Effective URL: override if set, otherwise default. Safe for synchronous remote calls. */
    fun resolveUrl(providerId: String): String

    suspend fun setUrl(providerId: String, url: String)

    suspend fun resetToDefault(providerId: String)
}
