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
        Log.d(TAG, "Requesting tip height for $network")
        try {
            val body = apiProvider.get(network).getTipHeight(config.heightUrl(network))
            val height = parseMsTipHeight(body.string())
            Log.i(TAG, "tip height succeeded for $network height=$height")
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
        Log.d(TAG, "Requesting address balance for $network")
        try {
            val balance = apiProvider.get(network).getAddress(address).toBalance()
            Log.i(TAG, "address balance succeeded for $network confirmed=${balance.confirmedSatoshis}")
            return balance
        } catch (e: HttpException) {
            if (isMsAddressNotFound(e.code())) {
                Log.i(TAG, "address not found on $network; treating as zero balance")
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
        Log.d(TAG, "Requesting address transactions for $network afterTxid=$afterTxid")
        try {
            val api = apiProvider.get(network)
            val txs = if (afterTxid == null) {
                api.getAddressTransactions(address)
            } else {
                api.getAddressTransactionsChain(address, afterTxid)
            }
            val page = txs.toTransactionPage(address)
            Log.i(TAG, "address transactions succeeded for $network count=${page.transactions.size}")
            return page
        } catch (e: HttpException) {
            if (isMsAddressNotFound(e.code())) {
                Log.i(TAG, "address not found on $network; treating as empty transactions")
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
        Log.d(TAG, "Requesting address UTXOs for $network")
        try {
            val utxos = apiProvider.get(network).getAddressUtxos(address).map { it.toDomain() }
            Log.i(TAG, "address UTXOs succeeded for $network count=${utxos.size}")
            return utxos
        } catch (e: HttpException) {
            if (isMsAddressNotFound(e.code())) {
                Log.i(TAG, "address not found on $network; treating as empty UTXOs")
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
        Log.d(TAG, "Requesting transaction hex for $network")
        try {
            val hex = apiProvider.get(network).getTransactionHex(txid).string().trim()
            if (hex.isEmpty()) error("empty transaction hex")
            Log.i(TAG, "transaction hex succeeded for $network")
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
        Log.d(TAG, "Broadcasting transaction for $network")
        val body = rawTxHex.toRequestBody(TX_HEX_MEDIA_TYPE)
        try {
            val txid = apiProvider.get(network).broadcastTransaction(body).string().trim()
            if (txid.isEmpty()) error("empty broadcast response")
            Log.i(TAG, "broadcast succeeded for $network")
            return txid
        } catch (e: HttpException) {
            val detail = e.response()?.errorBody()?.string()?.trim().orEmpty()
            Log.e(TAG, "broadcast failed for $network: ${detail.ifEmpty { e.message }}", e)
            error(detail.ifEmpty { "Could not broadcast transaction" })
        } catch (e: Exception) {
            Log.e(TAG, "broadcast failed for $network: ${e.message}", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "Ms"
        val TX_HEX_MEDIA_TYPE = "text/plain".toMediaType()
    }
}
