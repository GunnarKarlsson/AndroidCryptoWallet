package network.bahn.androidcryptowallet.data.remote.blockscout

import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPage
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor

interface EthereumTransactionRemoteDataSource {
    suspend fun getAddressTransactions(
        network: EvmNetwork,
        address: String,
        afterCursor: EvmTransactionPaginationCursor? = null,
    ): EvmTransactionPage
}
