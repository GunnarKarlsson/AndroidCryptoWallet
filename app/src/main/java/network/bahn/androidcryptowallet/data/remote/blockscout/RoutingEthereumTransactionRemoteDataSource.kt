package network.bahn.androidcryptowallet.data.remote.blockscout

import network.bahn.androidcryptowallet.data.remote.evm.EvmChainCatalog
import network.bahn.androidcryptowallet.data.remote.evm.EvmExplorerKind
import network.bahn.androidcryptowallet.data.remote.etherscan.EtherscanEthereumTransactionRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingEthereumTransactionRemoteDataSource @Inject constructor(
    private val catalog: EvmChainCatalog,
    private val blockscout: BlockscoutEthereumTransactionRemoteDataSource,
    private val etherscan: EtherscanEthereumTransactionRemoteDataSource,
) : EthereumTransactionRemoteDataSource {
    override suspend fun getAddressTransactions(
        network: EvmNetwork,
        address: String,
        afterCursor: EthereumTransactionPaginationCursor?,
    ): EthereumTransactionPage = when (catalog.explorerKind(network)) {
        EvmExplorerKind.BLOCKSCOUT -> blockscout.getAddressTransactions(network, address, afterCursor)
        EvmExplorerKind.ETHERSCAN -> etherscan.getAddressTransactions(network, address, afterCursor)
    }
}
