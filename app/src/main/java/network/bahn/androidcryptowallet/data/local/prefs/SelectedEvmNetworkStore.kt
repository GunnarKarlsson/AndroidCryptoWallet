package network.bahn.androidcryptowallet.data.local.prefs

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork

interface SelectedEvmNetworkStore {
    fun selectedNetwork(family: EvmFamily): Flow<EvmNetwork>
    suspend fun setNetwork(family: EvmFamily, network: EvmNetwork)
}
