package network.bahn.androidcryptowallet.data.remote.blockscout

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor

interface EthereumTransactionRemoteDataSource {
    suspend fun getAddressTransactions(
        network: EthereumNetwork,
        address: String,
        afterCursor: EthereumTransactionPaginationCursor? = null,
    ): EthereumTransactionPage
}
