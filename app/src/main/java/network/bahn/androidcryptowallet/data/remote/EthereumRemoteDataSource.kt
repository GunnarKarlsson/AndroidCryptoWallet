package network.bahn.androidcryptowallet.data.remote

import network.bahn.androidcryptowallet.domain.model.EthereumAddressBalance
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork

interface EthereumRemoteDataSource {
    suspend fun getAddressBalance(
        network: EthereumNetwork,
        address: String,
    ): EthereumAddressBalance
}
