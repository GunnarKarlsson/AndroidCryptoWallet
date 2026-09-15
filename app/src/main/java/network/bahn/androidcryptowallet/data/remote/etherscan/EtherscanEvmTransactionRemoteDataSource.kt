package network.bahn.androidcryptowallet.data.remote.etherscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.data.remote.evm.EvmChainCatalog
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPage
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EtherscanEvmTransactionRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
    private val catalog: EvmChainCatalog,
    private val json: Json,
) {
    suspend fun getAddressTransactions(
        network: EvmNetwork,
        address: String,
        afterCursor: EvmTransactionPaginationCursor?,
    ): EvmTransactionPage = withContext(Dispatchers.IO) {
        val page = afterCursor?.page ?: 1
        val url = catalog.explorerBaseUrl(network).toHttpUrl().newBuilder()
            .addQueryParameter("module", "account")
            .addQueryParameter("action", "txlist")
            .addQueryParameter("address", address)
            .addQueryParameter("startblock", "0")
            .addQueryParameter("endblock", "99999999")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("offset", ETHERSCAN_TX_PAGE_SIZE.toString())
            .addQueryParameter("sort", "desc")
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val responseBody = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Etherscan HTTP ${response.code}")
            }
            response.body.string()
        }
        val transactions = parseEtherscanTxList(responseBody, json)
        transactions.toTransactionPage(address, page)
    }
}
