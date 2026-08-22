package network.bahn.androidcryptowallet.data.remote

import network.bahn.androidcryptowallet.domain.model.BitcoinAddressBalance
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
import network.bahn.androidcryptowallet.domain.model.BitcoinUtxo

interface BitcoinRemoteDataSource {
    suspend fun getBlockCount(network: BitcoinNetwork): Long

    suspend fun getAddressBalance(
        network: BitcoinNetwork,
        address: String,
    ): BitcoinAddressBalance

    suspend fun getAddressTransactions(
        network: BitcoinNetwork,
        address: String,
        afterTxid: String?,
    ): BitcoinTransactionPage

    suspend fun getAddressUtxos(
        network: BitcoinNetwork,
        address: String,
    ): List<BitcoinUtxo>

    suspend fun getTransactionHex(
        network: BitcoinNetwork,
        txid: String,
    ): String

    suspend fun broadcastTransaction(
        network: BitcoinNetwork,
        rawTxHex: String,
    ): String
}
