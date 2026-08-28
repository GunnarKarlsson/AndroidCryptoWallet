package network.bahn.androidcryptowallet.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedEvmNetworkDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SelectedEvmNetworkStore {
    override fun selectedNetwork(family: EvmFamily): Flow<EvmNetwork> =
        dataStore.data.map { prefs -> resolveNetwork(prefs, family) }

    override suspend fun setNetwork(family: EvmFamily, network: EvmNetwork) {
        require(network.family == family) {
            "Network $network does not belong to family $family"
        }
        dataStore.edit { prefs ->
            prefs[keyFor(family)] = network.name
        }
    }

    private fun resolveNetwork(prefs: Preferences, family: EvmFamily): EvmNetwork {
        val storedName = prefs[keyFor(family)]
            ?: legacyStoredName(prefs, family)
        val parsed = storedName
            ?.let { runCatching { EvmNetwork.valueOf(it) }.getOrNull() }
            ?.takeIf { it.family == family }
        return parsed ?: defaultNetwork(family)
    }

    private fun legacyStoredName(prefs: Preferences, family: EvmFamily): String? =
        if (family == EvmFamily.ETHEREUM) prefs[LEGACY_KEY_SELECTED_ETHEREUM_NETWORK] else null

    private fun defaultNetwork(family: EvmFamily): EvmNetwork = when (family) {
        EvmFamily.ETHEREUM -> EvmNetwork.SEPOLIA
        EvmFamily.BSC -> EvmNetwork.BSC_TESTNET
        EvmFamily.POLYGON -> EvmNetwork.POLYGON_AMOY
    }

    private companion object {
        val LEGACY_KEY_SELECTED_ETHEREUM_NETWORK =
            stringPreferencesKey("selected_ethereum_network")

        fun keyFor(family: EvmFamily) = stringPreferencesKey("selected_evm_network_${family.name}")
    }
}
