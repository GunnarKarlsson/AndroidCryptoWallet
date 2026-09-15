package network.bahn.androidcryptowallet.data.remote.blockscout

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
class BlockscoutEvmTransactionRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
    private val catalog: EvmChainCatalog,
    private val json: Json,
) {
    suspend fun getAddressTransactions(
        network: EvmNetwork,
        address: String,
        afterCursor: EvmTransactionPaginationCursor?,
    ): EvmTransactionPage = withContext(Dispatchers.IO) {
        val urlBuilder = "${catalog.explorerBaseUrl(network)}/addresses/$address/transactions"
            .toHttpUrl()
            .newBuilder()
        afterCursor?.toQueryParams()?.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }
        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()
        val responseBody = client.newCall(request).execute().use { response ->
            if (response.code == 404) {
                return@withContext EvmTransactionPage(
                    transactions = emptyList(),
                    nextCursor = null,
                    hasMore = false,
                )
            }
            if (!response.isSuccessful) {
                error("Blockscout HTTP ${response.code}")
            }
            response.body.string()
        }
        val pageResponse = json.decodeFromString<BlockscoutTxPageResponse>(responseBody)
        pageResponse.toTransactionPage(address)
    }
}
