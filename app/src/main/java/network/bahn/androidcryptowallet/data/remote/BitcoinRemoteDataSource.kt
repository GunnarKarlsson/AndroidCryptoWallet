package network.bahn.androidcryptowallet.data.remote

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

interface BitcoinRemoteDataSource {
    suspend fun getBlockCount(network: BitcoinNetwork): Long
}
