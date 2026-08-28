package network.bahn.androidcryptowallet.data.remote.blockscout

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.data.remote.evm.EvmChainCatalog
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockscoutEthereumTransactionRemoteDataSource @Inject constructor(
    private val client: OkHttpClient,
    private val catalog: EvmChainCatalog,
    private val json: Json,
) : EthereumTransactionRemoteDataSource {
    override suspend fun getAddressTransactions(
        network: EvmNetwork,
        address: String,
        afterCursor: EthereumTransactionPaginationCursor?,
    ): EthereumTransactionPage = withContext(Dispatchers.IO) {
        Log.d(TAG, "Requesting address transactions for $network afterCursor=${afterCursor != null}")
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
                Log.i(TAG, "address not found on $network; treating as empty transactions")
                return@withContext EthereumTransactionPage(
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
        val page = pageResponse.toTransactionPage(address)
        Log.i(TAG, "address transactions succeeded for $network count=${page.transactions.size}")
        page
    }

    companion object {
        private const val TAG = "EthTxRemote"
    }
}
