package network.bahn.androidcryptowallet.data.remote

import network.bahn.androidcryptowallet.domain.model.EvmAddressBalance
import network.bahn.androidcryptowallet.domain.model.EvmFeeData
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import java.math.BigInteger

interface EthereumRemoteDataSource {
    suspend fun getAddressBalance(
        network: EvmNetwork,
        address: String,
    ): EvmAddressBalance

    suspend fun getTransactionCount(
        network: EvmNetwork,
        address: String,
    ): Long

    suspend fun estimateGas(
        network: EvmNetwork,
        from: String,
        to: String,
        valueWei: BigInteger,
    ): Long

    suspend fun getFeeData(network: EvmNetwork): EvmFeeData

    /** Broadcast a signed raw transaction; returns the transaction hash. */
    suspend fun sendRawTransaction(
        network: EvmNetwork,
        signedRawHex: String,
    ): String
}
