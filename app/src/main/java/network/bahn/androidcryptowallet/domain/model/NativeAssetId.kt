package network.bahn.androidcryptowallet.domain.model

/**
 * Mainnet native assets used for USD valuation.
 * Testnet holdings use the same IDs as an approximate reference.
 */
enum class NativeAssetId {
    BITCOIN,
    ETHEREUM,
    BINANCECOIN,
    POLYGON,
    AVALANCHE,
    ;

    companion object {
        fun fromStorageKey(key: String): NativeAssetId? =
            runCatching { valueOf(key) }.getOrNull()
    }
}

fun PortfolioHoldingDestination.nativeAssetId(): NativeAssetId = when (this) {
    PortfolioHoldingDestination.Bitcoin -> NativeAssetId.BITCOIN
    is PortfolioHoldingDestination.Evm -> family.nativeAssetId()
}

fun EvmFamily.nativeAssetId(): NativeAssetId = when (this) {
    EvmFamily.ETHEREUM,
    EvmFamily.ARBITRUM,
    EvmFamily.BASE,
    EvmFamily.OPTIMISM,
    -> NativeAssetId.ETHEREUM
    EvmFamily.BSC -> NativeAssetId.BINANCECOIN
    EvmFamily.POLYGON -> NativeAssetId.POLYGON
    EvmFamily.AVALANCHE -> NativeAssetId.AVALANCHE
}
