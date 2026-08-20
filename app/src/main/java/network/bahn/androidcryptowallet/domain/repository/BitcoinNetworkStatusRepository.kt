package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinNetworkStatus

interface BitcoinNetworkStatusRepository {
    fun observeStatus(): Flow<BitcoinNetworkStatus?>
    fun selectedNetwork(): Flow<BitcoinNetwork>
    suspend fun setNetwork(network: BitcoinNetwork)
    suspend fun refreshBlockHeight()
}
