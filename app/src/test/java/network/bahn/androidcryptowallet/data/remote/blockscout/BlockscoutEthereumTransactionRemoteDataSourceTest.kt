package network.bahn.androidcryptowallet.data.remote.blockscout

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.data.remote.evm.EvmChainCatalog
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockscoutEthereumTransactionRemoteDataSourceTest {
    private val catalog = EvmChainCatalog(
        rpcUrls = mapOf(
            EvmNetwork.SEPOLIA to "https://ethereum-sepolia-rpc.publicnode.com",
            EvmNetwork.BSC_TESTNET to "https://data-seed-prebsc-1-s1.bnbchain.org:8545",
        ),
        explorerBaseUrls = mapOf(
            EvmNetwork.SEPOLIA to "https://eth-sepolia.blockscout.com/api/v2",
            EvmNetwork.BSC_TESTNET to "https://api-testnet.bscscan.com/api",
        ),
    )
    private val remote = BlockscoutEthereumTransactionRemoteDataSource(
        client = OkHttpClient(),
        catalog = catalog,
        json = Json { ignoreUnknownKeys = true },
    )

    @Test
    fun getAddressTransactions_returnsEmptyPageForBscUntilExplorerAdapter() = runTest {
        val page = remote.getAddressTransactions(
            network = EvmNetwork.BSC_TESTNET,
            address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
            afterCursor = null,
        )

        assertTrue(page.transactions.isEmpty())
        assertFalse(page.hasMore)
        assertEquals(null, page.nextCursor)
    }
}
