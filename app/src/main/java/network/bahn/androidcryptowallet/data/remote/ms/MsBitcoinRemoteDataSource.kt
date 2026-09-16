package network.bahn.androidcryptowallet.data.remote.ms

import android.util.Log
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.BitcoinAddressBalance
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
import network.bahn.androidcryptowallet.domain.model.BitcoinUtxo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MsBitcoinRemoteDataSource @Inject constructor(
    private val apiProvider: MsApiProvider,
    private val config: MsBitcoinConfig,
) : BitcoinRemoteDataSource {
    override suspend fun getBlockCount(network: BitcoinNetwork): Long {
        try {
            val body = apiProvider.get(network).getTipHeight(config.heightUrl(network))
            val height = parseMsTipHeight(body.string())
            return height
        } catch (e: Exception) {
            Log.e(TAG, "tip height failed for $network: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getAddressBalance(
        network: BitcoinNetwork,
        address: String,
    ): BitcoinAddressBalance {
        try {
            val balance = apiProvider.get(network).getAddress(address).toBalance()
            return balance
        } catch (e: HttpException) {
            if (isMsAddressNotFound(e.code())) {
                return BitcoinAddressBalance(confirmedSatoshis = 0L)
            }
            Log.e(TAG, "address balance failed for $network: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "address balance failed for $network: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getAddressTransactions(
        network: BitcoinNetwork,
        address: String,
        afterTxid: String?,
    ): BitcoinTransactionPage {
        try {
            val api = apiProvider.get(network)
            val txs = if (afterTxid == null) {
                api.getAddressTransactions(address)
            } else {
                api.getAddressTransactionsChain(address, afterTxid)
            }
            val page = txs.toTransactionPage(address)
            return page
        } catch (e: HttpException) {
            if (isMsAddressNotFound(e.code())) {
                return BitcoinTransactionPage(
                    transactions = emptyList(),
                    lastConfirmedTxid = null,
                    hasMore = false,
                )
            }
            Log.e(TAG, "address transactions failed for $network: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "address transactions failed for $network: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getAddressUtxos(
        network: BitcoinNetwork,
        address: String,
    ): List<BitcoinUtxo> {
        try {
            val utxos = apiProvider.get(network).getAddressUtxos(address).map { it.toDomain() }
            return utxos
        } catch (e: HttpException) {
            if (isMsAddressNotFound(e.code())) {
                return emptyList()
            }
            Log.e(TAG, "address UTXOs failed for $network: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "address UTXOs failed for $network: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getTransactionHex(
        network: BitcoinNetwork,
        txid: String,
    ): String {
        try {
            val hex = apiProvider.get(network).getTransactionHex(txid).string().trim()
            if (hex.isEmpty()) error("empty transaction hex")
            return hex
        } catch (e: Exception) {
            Log.e(TAG, "transaction hex failed for $network: ${e.message}", e)
            throw e
        }
    }

    override suspend fun broadcastTransaction(
        network: BitcoinNetwork,
        rawTxHex: String,
    ): String {
        // Never log [rawTxHex]: it is a signed Bitcoin transaction.
        val body = rawTxHex.toRequestBody(TX_HEX_MEDIA_TYPE)
        try {
            val txid = apiProvider.get(network).broadcastTransaction(body).string().trim()
            if (txid.isEmpty()) error("empty broadcast response")
            return txid
        } catch (e: HttpException) {
            val detail = e.response()?.errorBody()?.string()?.trim().orEmpty()
            Log.e(TAG, "broadcast failed for $network (${e.code()})")
            error(detail.ifEmpty { "Could not broadcast transaction" })
        } catch (e: Exception) {
            Log.e(TAG, "broadcast failed for $network (${e.javaClass.simpleName})")
            throw e
        }
    }

    private companion object {
        const val TAG = "Ms"
        val TX_HEX_MEDIA_TYPE = "text/plain".toMediaType()
    }
}
