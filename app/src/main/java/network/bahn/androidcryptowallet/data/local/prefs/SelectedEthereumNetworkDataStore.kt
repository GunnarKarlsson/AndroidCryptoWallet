package network.bahn.androidcryptowallet.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedEthereumNetworkDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SelectedEthereumNetworkStore {
    override val selectedNetwork: Flow<EvmNetwork> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_ETHEREUM_NETWORK]
            ?.let { runCatching { EvmNetwork.valueOf(it) }.getOrNull() }
            ?: EvmNetwork.SEPOLIA
    }

    override suspend fun setNetwork(network: EvmNetwork) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_ETHEREUM_NETWORK] = network.name
        }
    }

    private companion object {
        val KEY_SELECTED_ETHEREUM_NETWORK = stringPreferencesKey("selected_ethereum_network")
    }
}
