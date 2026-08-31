package network.bahn.androidcryptowallet.domain.model

enum class WalletNetworkMode {
    MAINNET,
    TESTNET,
    ;

    fun bitcoinNetwork(): BitcoinNetwork = when (this) {
        MAINNET -> BitcoinNetwork.MAINNET
        TESTNET -> BitcoinNetwork.TESTNET4
    }

    fun defaultEvmNetwork(family: EvmFamily): EvmNetwork = when (this) {
        MAINNET -> when (family) {
            EvmFamily.ETHEREUM -> EvmNetwork.MAINNET
            EvmFamily.BSC -> EvmNetwork.BSC_MAINNET
            EvmFamily.POLYGON -> EvmNetwork.POLYGON_MAINNET
            EvmFamily.ARBITRUM -> EvmNetwork.ARBITRUM_MAINNET
            EvmFamily.BASE -> EvmNetwork.BASE_MAINNET
            EvmFamily.OPTIMISM -> EvmNetwork.OPTIMISM_MAINNET
            EvmFamily.AVALANCHE -> EvmNetwork.AVALANCHE_MAINNET
        }
        TESTNET -> when (family) {
            EvmFamily.ETHEREUM -> EvmNetwork.SEPOLIA
            EvmFamily.BSC -> EvmNetwork.BSC_TESTNET
            EvmFamily.POLYGON -> EvmNetwork.POLYGON_AMOY
            EvmFamily.ARBITRUM -> EvmNetwork.ARBITRUM_SEPOLIA
            EvmFamily.BASE -> EvmNetwork.BASE_SEPOLIA
            EvmFamily.OPTIMISM -> EvmNetwork.OPTIMISM_SEPOLIA
            EvmFamily.AVALANCHE -> EvmNetwork.AVALANCHE_FUJI
        }
    }

    fun matches(bitcoinNetwork: BitcoinNetwork): Boolean =
        bitcoinNetwork() == bitcoinNetwork

    fun matches(evmNetwork: EvmNetwork): Boolean = when (this) {
        MAINNET -> evmNetwork.isMainnet
        TESTNET -> !evmNetwork.isMainnet
    }

    companion object {
        fun fromBitcoinNetwork(network: BitcoinNetwork): WalletNetworkMode = when (network) {
            BitcoinNetwork.MAINNET -> MAINNET
            BitcoinNetwork.TESTNET4 -> TESTNET
        }
    }
}

fun BitcoinNetwork.portfolioHeadline(): String = "Bitcoin $label (BTC)"

fun EvmNetwork.portfolioHeadline(): String =
    "${family.displayName()} $label ($nativeSymbol)"

private fun EvmFamily.displayName(): String = when (this) {
    EvmFamily.ETHEREUM -> "Ethereum"
    EvmFamily.BSC -> "BSC"
    EvmFamily.POLYGON -> "Polygon"
    EvmFamily.ARBITRUM -> "Arbitrum"
    EvmFamily.BASE -> "Base"
    EvmFamily.OPTIMISM -> "Optimism"
    EvmFamily.AVALANCHE -> "Avalanche"
}
