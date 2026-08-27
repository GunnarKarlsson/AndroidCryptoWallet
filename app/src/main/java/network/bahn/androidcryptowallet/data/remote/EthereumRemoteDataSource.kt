package network.bahn.androidcryptowallet.data.remote

import network.bahn.androidcryptowallet.domain.model.EthereumAddressBalance
import network.bahn.androidcryptowallet.domain.model.EthereumFeeData
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import java.math.BigInteger

interface EthereumRemoteDataSource {
    suspend fun getAddressBalance(
        network: EthereumNetwork,
        address: String,
    ): EthereumAddressBalance

    suspend fun getTransactionCount(
        network: EthereumNetwork,
        address: String,
    ): Long

    suspend fun estimateGas(
        network: EthereumNetwork,
        from: String,
        to: String,
        valueWei: BigInteger,
    ): Long

    suspend fun getFeeData(network: EthereumNetwork): EthereumFeeData

    /** Broadcast a signed raw transaction; returns the transaction hash. */
    suspend fun sendRawTransaction(
        network: EthereumNetwork,
        signedRawHex: String,
    ): String
}
