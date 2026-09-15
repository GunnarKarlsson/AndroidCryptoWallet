package network.bahn.androidcryptowallet.domain.model

data class AssetPrice(
    val assetId: NativeAssetId,
    val priceUsdMicros: Long,
    val updatedAtMillis: Long,
)
