package network.bahn.androidcryptowallet.data.remote.alchemy

import android.util.Log
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlchemyBitcoinRemoteDataSource @Inject constructor(
    private val apiFactory: AlchemyBitcoinJsonRpcApiFactory,
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

    private companion object {
        const val TAG = "Alchemy"
    }
}
