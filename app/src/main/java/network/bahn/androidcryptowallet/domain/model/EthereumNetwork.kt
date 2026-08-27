package network.bahn.androidcryptowallet.domain.model

enum class EthereumNetwork(val label: String, val chainId: Long) {
    SEPOLIA("Sepolia", chainId = 11_155_111L),
    MAINNET("Mainnet", chainId = 1L),
}
