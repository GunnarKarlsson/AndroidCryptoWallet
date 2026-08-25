package network.bahn.androidcryptowallet.data.local.prefs

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork

interface SelectedEthereumNetworkStore {
    val selectedNetwork: Flow<EthereumNetwork>
    suspend fun setNetwork(network: EthereumNetwork)
}
