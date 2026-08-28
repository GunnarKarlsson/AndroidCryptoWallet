package network.bahn.androidcryptowallet.data.local.prefs

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.EvmNetwork

interface SelectedEthereumNetworkStore {
    val selectedNetwork: Flow<EvmNetwork>
    suspend fun setNetwork(network: EvmNetwork)
}
