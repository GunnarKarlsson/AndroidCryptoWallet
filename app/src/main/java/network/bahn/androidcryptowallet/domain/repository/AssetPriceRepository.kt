package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.AssetPrice
import network.bahn.androidcryptowallet.domain.model.NativeAssetId

interface AssetPriceRepository {
    fun observePrices(): Flow<Map<NativeAssetId, AssetPrice>>

    suspend fun refreshPrices()
}
