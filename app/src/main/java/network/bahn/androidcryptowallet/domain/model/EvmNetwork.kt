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
    ;

    companion object {
        fun networksFor(family: EvmFamily): List<EvmNetwork> =
            entries.filter { it.family == family }
    }
}
