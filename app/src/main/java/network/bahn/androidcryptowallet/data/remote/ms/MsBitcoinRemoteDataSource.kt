package network.bahn.androidcryptowallet.data.remote.ms

import android.util.Log
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.domain.model.BitcoinAddressBalance
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
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

    private companion object {
        const val TAG = "Ms"
    }
}
