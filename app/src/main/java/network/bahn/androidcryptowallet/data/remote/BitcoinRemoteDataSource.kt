package network.bahn.androidcryptowallet.data.remote

import network.bahn.androidcryptowallet.domain.model.BitcoinAddressBalance
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

interface BitcoinRemoteDataSource {
    suspend fun getBlockCount(network: BitcoinNetwork): Long

    suspend fun getAddressBalance(
        network: BitcoinNetwork,
        address: String,
    ): BitcoinAddressBalance
}
