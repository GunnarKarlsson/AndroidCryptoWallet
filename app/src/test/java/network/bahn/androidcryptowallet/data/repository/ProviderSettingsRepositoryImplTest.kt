package network.bahn.androidcryptowallet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProviderSettingsRepositoryImplTest {
    @Test
    fun resolveUrl_returnsDefaultWhenNoOverride() = runTest {
        val repository = createRepository()
        assertEquals(
            DefaultProviderCatalog().defaultUrl(ProviderIds.BITCOIN_TESTNET4),
            repository.resolveUrl(ProviderIds.BITCOIN_TESTNET4),
        )
    }

    @Test
    fun setUrl_persistsOverrideAndResetRestoresDefault() = runTest {
        val repository = createRepository()
        val providerId = ProviderIds.evmRpc(
            network.bahn.androidcryptowallet.domain.model.EvmNetwork.SEPOLIA,
        )
        val customUrl = "https://custom-sepolia.example"

        repository.setUrl(providerId, customUrl)
        assertEquals(customUrl, repository.resolveUrl(providerId))

        val providers = repository.observeProviders().first()
        val provider = providers.single { it.id == providerId }
        assertTrue(provider.isOverridden)
        assertEquals(customUrl, provider.currentUrl)

        repository.resetToDefault(providerId)
        assertEquals(
            DefaultProviderCatalog().defaultUrl(providerId),
            repository.resolveUrl(providerId),
        )

        val resetProviders = repository.observeProviders().first()
        val resetProvider = resetProviders.single { it.id == providerId }
        assertFalse(resetProvider.isOverridden)
    }

    private fun createRepository(): ProviderSettingsRepositoryImpl {
        val dataStore = createTestDataStore()
        return ProviderSettingsRepositoryImpl(
            dataStore = dataStore,
            defaultProviderCatalog = DefaultProviderCatalog(),
        )
    }

    private fun createTestDataStore(): DataStore<Preferences> {
        val file = File.createTempFile("provider_settings", ".preferences_pb")
        file.deleteOnExit()
        return PreferenceDataStoreFactory.create(
            produceFile = { file },
        )
    }
}
