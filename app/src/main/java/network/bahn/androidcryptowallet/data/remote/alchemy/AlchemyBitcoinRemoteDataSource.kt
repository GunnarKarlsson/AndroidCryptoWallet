package network.bahn.androidcryptowallet.data.remote.alchemy

import android.util.Log
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.BitcoinAddressBalance
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
import network.bahn.androidcryptowallet.domain.model.BitcoinUtxo
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlchemyBitcoinRemoteDataSource @Inject constructor(
    private val apiFactory: AlchemyBitcoinJsonRpcApiFactory,
    private val utxoApiFactory: AlchemyUtxoApiFactory,
    private val config: AlchemyBitcoinConfig,
) : BitcoinRemoteDataSource {
    override suspend fun getBlockCount(network: BitcoinNetwork): Long {
        Log.d(TAG, "Requesting getblockcount for $network")
        try {
            val response = apiFactory.get(network).call(
                apiKey = config.apiKey,
                body = JsonRpcRequest(method = "getblockcount"),
            )
            val rpcError = response.error
            if (rpcError != null) {
                error("JSON-RPC ${rpcError.code}: ${rpcError.message}")
            }
            val height = response.result ?: error("empty result")
            Log.i(TAG, "getblockcount succeeded for $network height=$height")
            return height
        } catch (e: Exception) {
            Log.e(TAG, "getblockcount failed for $network: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getAddressBalance(
        network: BitcoinNetwork,
        address: String,
    ): BitcoinAddressBalance {
        Log.d(TAG, "Requesting address balance for $network")
        try {
            val response = utxoApiFactory.get(network).getAddress(
                apiKey = config.apiKey,
                address = address,
            )
            val balance = BitcoinAddressBalance(
                confirmedSatoshis = response.balance.toSatoshis(),
                unconfirmedSatoshis = response.unconfirmedBalance.toSatoshis(),
            )
            Log.i(TAG, "address balance succeeded for $network confirmed=${balance.confirmedSatoshis}")
            return balance
        } catch (e: HttpException) {
            if (e.code() == 404 || e.code() == 400) {
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
        throw UnsupportedOperationException("Alchemy does not implement address transactions")
    }

    override suspend fun getAddressUtxos(
        network: BitcoinNetwork,
        address: String,
    ): List<BitcoinUtxo> {
        throw UnsupportedOperationException("Alchemy does not implement address UTXOs")
    }

    override suspend fun getTransactionHex(
        network: BitcoinNetwork,
        txid: String,
    ): String {
        throw UnsupportedOperationException("Alchemy does not implement transaction hex")
    }

    override suspend fun broadcastTransaction(
        network: BitcoinNetwork,
        rawTxHex: String,
    ): String {
        throw UnsupportedOperationException("Alchemy does not implement transaction broadcast")
    }

    private companion object {
        const val TAG = "Alchemy"

        fun String?.toSatoshis(): Long = this?.toLongOrNull() ?: 0L
    }
}

