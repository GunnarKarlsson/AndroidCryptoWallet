package network.bahn.androidcryptowallet.domain.model

enum class EvmNetwork(
    val family: EvmFamily,
    val label: String,
    val chainId: Long,
    val nativeSymbol: String,
) {
    SEPOLIA(EvmFamily.ETHEREUM, "Sepolia", chainId = 11_155_111L, nativeSymbol = "ETH"),
    MAINNET(EvmFamily.ETHEREUM, "Mainnet", chainId = 1L, nativeSymbol = "ETH"),
    ;

    companion object {
        fun networksFor(family: EvmFamily): List<EvmNetwork> =
            entries.filter { it.family == family }
    }
}
