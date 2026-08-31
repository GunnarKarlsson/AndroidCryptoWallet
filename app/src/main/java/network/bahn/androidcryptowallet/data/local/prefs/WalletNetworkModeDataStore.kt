package network.bahn.androidcryptowallet.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletNetworkModeDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val selectedBitcoinNetworkStore: SelectedBitcoinNetworkStore,
    private val selectedEvmNetworkStore: SelectedEvmNetworkStore,
) : WalletNetworkModeStore {
    override fun observeMode(): Flow<WalletNetworkMode> = dataStore.data.map { prefs ->
        prefs[KEY_WALLET_NETWORK_MODE]
            ?.let { runCatching { WalletNetworkMode.valueOf(it) }.getOrNull() }
            ?: WalletNetworkMode.TESTNET
    }

    override suspend fun setMode(mode: WalletNetworkMode) {
        dataStore.edit { prefs ->
            prefs[KEY_WALLET_NETWORK_MODE] = mode.name
        }
        selectedBitcoinNetworkStore.setNetwork(mode.bitcoinNetwork())
        EvmFamily.entries.forEach { family ->
            selectedEvmNetworkStore.setNetwork(family, mode.defaultEvmNetwork(family))
        }
    }

    private companion object {
        val KEY_WALLET_NETWORK_MODE = stringPreferencesKey("wallet_network_mode")
    }
}
