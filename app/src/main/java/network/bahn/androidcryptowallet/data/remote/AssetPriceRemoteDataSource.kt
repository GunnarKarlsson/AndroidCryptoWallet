package network.bahn.androidcryptowallet.data.remote

import network.bahn.androidcryptowallet.domain.model.NativeAssetId

interface AssetPriceRemoteDataSource {
    suspend fun fetchUsdMicros(): Map<NativeAssetId, Long>
}
