package network.bahn.androidcryptowallet.data.remote.blockscout

import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor

interface EthereumTransactionRemoteDataSource {
    suspend fun getAddressTransactions(
        network: EvmNetwork,
        address: String,
        afterCursor: EthereumTransactionPaginationCursor? = null,
    ): EthereumTransactionPage
}
