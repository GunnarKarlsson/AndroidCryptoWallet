package network.bahn.androidcryptowallet.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedEthereumNetworkDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SelectedEthereumNetworkStore {
    override val selectedNetwork: Flow<EthereumNetwork> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_ETHEREUM_NETWORK]
            ?.let { runCatching { EthereumNetwork.valueOf(it) }.getOrNull() }
            ?: EthereumNetwork.SEPOLIA
    }

    override suspend fun setNetwork(network: EthereumNetwork) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_ETHEREUM_NETWORK] = network.name
        }
    }

    private companion object {
        val KEY_SELECTED_ETHEREUM_NETWORK = stringPreferencesKey("selected_ethereum_network")
    }
}
