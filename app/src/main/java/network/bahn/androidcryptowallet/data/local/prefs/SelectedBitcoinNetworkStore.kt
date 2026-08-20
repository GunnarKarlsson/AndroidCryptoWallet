package network.bahn.androidcryptowallet.data.local.prefs

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

interface SelectedBitcoinNetworkStore {
    val selectedNetwork: Flow<BitcoinNetwork>
    suspend fun setNetwork(network: BitcoinNetwork)
}
