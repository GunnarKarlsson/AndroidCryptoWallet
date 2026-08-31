package network.bahn.androidcryptowallet.domain.model

enum class EvmNetwork(
    val family: EvmFamily,
    val label: String,
    val chainId: Long,
    val nativeSymbol: String,
) {
    SEPOLIA(EvmFamily.ETHEREUM, "Sepolia", chainId = 11_155_111L, nativeSymbol = "ETH"),
    MAINNET(EvmFamily.ETHEREUM, "Mainnet", chainId = 1L, nativeSymbol = "ETH"),
    BSC_TESTNET(EvmFamily.BSC, "BSC Testnet", chainId = 97L, nativeSymbol = "tBNB"),
    BSC_MAINNET(EvmFamily.BSC, "BSC Mainnet", chainId = 56L, nativeSymbol = "BNB"),
    POLYGON_AMOY(EvmFamily.POLYGON, "Amoy Testnet", chainId = 80_002L, nativeSymbol = "POL"),
    POLYGON_MAINNET(EvmFamily.POLYGON, "Mainnet", chainId = 137L, nativeSymbol = "POL"),
    ARBITRUM_SEPOLIA(EvmFamily.ARBITRUM, "Sepolia", chainId = 421_614L, nativeSymbol = "ETH"),
    ARBITRUM_MAINNET(EvmFamily.ARBITRUM, "Mainnet", chainId = 42_161L, nativeSymbol = "ETH"),
    BASE_SEPOLIA(EvmFamily.BASE, "Sepolia", chainId = 84_532L, nativeSymbol = "ETH"),
    BASE_MAINNET(EvmFamily.BASE, "Mainnet", chainId = 8453L, nativeSymbol = "ETH"),
    OPTIMISM_SEPOLIA(EvmFamily.OPTIMISM, "Sepolia", chainId = 11_155_420L, nativeSymbol = "ETH"),
    OPTIMISM_MAINNET(EvmFamily.OPTIMISM, "Mainnet", chainId = 10L, nativeSymbol = "ETH"),
    AVALANCHE_FUJI(EvmFamily.AVALANCHE, "Fuji", chainId = 43_113L, nativeSymbol = "AVAX"),
    AVALANCHE_MAINNET(EvmFamily.AVALANCHE, "Mainnet", chainId = 43_114L, nativeSymbol = "AVAX"),
    ;

    val isMainnet: Boolean
        get() = when (this) {
            MAINNET,
            BSC_MAINNET,
            POLYGON_MAINNET,
            ARBITRUM_MAINNET,
            BASE_MAINNET,
            OPTIMISM_MAINNET,
            AVALANCHE_MAINNET,
            -> true
            else -> false
        }

    companion object {
        fun networksFor(family: EvmFamily): List<EvmNetwork> =
            entries.filter { it.family == family }
    }
}
