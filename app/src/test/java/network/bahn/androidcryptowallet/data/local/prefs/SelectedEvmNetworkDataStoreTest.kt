package network.bahn.androidcryptowallet.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SelectedEvmNetworkDataStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun selectedNetwork_defaultsToSepoliaForEthereum() = runTest {
        val store = createStore()
        assertEquals(EvmNetwork.SEPOLIA, store.selectedNetwork(EvmFamily.ETHEREUM).first())
    }

    @Test
    fun selectedNetwork_defaultsToBscTestnetForBsc() = runTest {
        val store = createStore()
        assertEquals(EvmNetwork.BSC_TESTNET, store.selectedNetwork(EvmFamily.BSC).first())
    }

    @Test
    fun setNetwork_persistsPerFamilyKey() = runTest {
        val store = createStore()
        store.setNetwork(EvmFamily.ETHEREUM, EvmNetwork.MAINNET)
        assertEquals(EvmNetwork.MAINNET, store.selectedNetwork(EvmFamily.ETHEREUM).first())
    }

    @Test
    fun selectedNetwork_readsLegacyEthereumKeyWhenFamilyKeyMissing() = runTest {
        val dataStore = createDataStore()
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("selected_ethereum_network")] = EvmNetwork.MAINNET.name
        }
        val store = SelectedEvmNetworkDataStore(dataStore)
        assertEquals(EvmNetwork.MAINNET, store.selectedNetwork(EvmFamily.ETHEREUM).first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun setNetwork_rejectsNetworkFromWrongFamily() = runTest {
        val store = createStore()
        store.setNetwork(EvmFamily.BSC, EvmNetwork.MAINNET)
    }

    private fun createStore(): SelectedEvmNetworkDataStore =
        SelectedEvmNetworkDataStore(createDataStore())

    private fun createDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder.newFolder(), "test_prefs.preferences_pb") },
        )
}
