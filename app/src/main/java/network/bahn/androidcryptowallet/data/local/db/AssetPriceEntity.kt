package network.bahn.androidcryptowallet.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import network.bahn.androidcryptowallet.domain.model.AssetPrice
import network.bahn.androidcryptowallet.domain.model.NativeAssetId

@Entity(tableName = "asset_price")
data class AssetPriceEntity(
    @PrimaryKey val assetId: String,
    val priceUsdMicros: Long,
    val updatedAtMillis: Long,
)

fun AssetPriceEntity.toDomain(): AssetPrice? {
    val id = NativeAssetId.fromStorageKey(assetId) ?: return null
    return AssetPrice(
        assetId = id,
        priceUsdMicros = priceUsdMicros,
        updatedAtMillis = updatedAtMillis,
    )
}
