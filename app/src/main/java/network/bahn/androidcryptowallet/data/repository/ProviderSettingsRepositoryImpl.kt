package network.bahn.androidcryptowallet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.model.ProviderSetting
import network.bahn.androidcryptowallet.domain.repository.ProviderSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val defaultProviderCatalog: DefaultProviderCatalog,
) : ProviderSettingsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var overridesCache: Map<String, String> = emptyMap()

    init {
        scope.launch {
            dataStore.data.collect { prefs ->
                overridesCache = readOverrides(prefs)
            }
        }
    }

    override fun observeProviders(): Flow<List<ProviderSetting>> =
        dataStore.data.map { prefs -> buildProviderList(readOverrides(prefs)) }

    override fun observeProvider(id: String): Flow<ProviderSetting?> =
        observeProviders().map { providers -> providers.find { it.id == id } }

    override fun resolveUrl(providerId: String): String =
        overridesCache[providerId] ?: defaultProviderCatalog.defaultUrl(providerId)

    override suspend fun setUrl(providerId: String, url: String) {
        defaultProviderCatalog.definition(providerId)
        val trimmed = url.trim()
        require(trimmed.isNotEmpty()) { "Provider URL cannot be blank" }
        dataStore.edit { prefs ->
            prefs[urlKey(providerId)] = trimmed
        }
        overridesCache = overridesCache + (providerId to trimmed)
    }

    override suspend fun resetToDefault(providerId: String) {
        defaultProviderCatalog.definition(providerId)
        dataStore.edit { prefs ->
            prefs.remove(urlKey(providerId))
        }
        overridesCache = overridesCache - providerId
    }

    private fun buildProviderList(overrides: Map<String, String>): List<ProviderSetting> =
        defaultProviderCatalog.allDefinitions().map { definition ->
            val override = overrides[definition.id]
            ProviderSetting(
                id = definition.id,
                groupLabel = definition.groupLabel,
                label = definition.label,
                currentUrl = override ?: definition.defaultUrl,
                defaultUrl = definition.defaultUrl,
                isOverridden = override != null,
            )
        }

    private fun readOverrides(prefs: Preferences): Map<String, String> =
        defaultProviderCatalog.allDefinitions().mapNotNull { definition ->
            prefs[urlKey(definition.id)]?.let { url -> definition.id to url }
        }.toMap()

    private fun urlKey(providerId: String) = stringPreferencesKey("provider_url_$providerId")
}
