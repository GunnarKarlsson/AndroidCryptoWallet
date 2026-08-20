package network.bahn.androidcryptowallet.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedBitcoinNetworkDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SelectedBitcoinNetworkStore {
    override val selectedNetwork: Flow<BitcoinNetwork> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_BITCOIN_NETWORK]
            ?.let { runCatching { BitcoinNetwork.valueOf(it) }.getOrNull() }
            ?: BitcoinNetwork.TESTNET4
    }

    override suspend fun setNetwork(network: BitcoinNetwork) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_BITCOIN_NETWORK] = network.name
        }
    }

    private companion object {
        val KEY_SELECTED_BITCOIN_NETWORK = stringPreferencesKey("selected_bitcoin_network")
    }
}
