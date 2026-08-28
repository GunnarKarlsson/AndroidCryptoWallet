package network.bahn.androidcryptowallet.data.remote.blockscout

import network.bahn.androidcryptowallet.data.remote.evm.EvmChainCatalog
import network.bahn.androidcryptowallet.data.remote.evm.EvmExplorerKind
import network.bahn.androidcryptowallet.data.remote.etherscan.EtherscanEvmTransactionRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPage
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingEvmTransactionRemoteDataSource @Inject constructor(
    private val catalog: EvmChainCatalog,
    private val blockscout: BlockscoutEvmTransactionRemoteDataSource,
    private val etherscan: EtherscanEvmTransactionRemoteDataSource,
) : EvmTransactionRemoteDataSource {
    override suspend fun getAddressTransactions(
        network: EvmNetwork,
        address: String,
        afterCursor: EvmTransactionPaginationCursor?,
    ): EvmTransactionPage = when (catalog.explorerKind(network)) {
        EvmExplorerKind.BLOCKSCOUT -> blockscout.getAddressTransactions(network, address, afterCursor)
        EvmExplorerKind.ETHERSCAN -> etherscan.getAddressTransactions(network, address, afterCursor)
    }
}
