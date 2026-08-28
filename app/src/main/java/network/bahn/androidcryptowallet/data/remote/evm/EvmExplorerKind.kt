package network.bahn.androidcryptowallet.data.remote.evm

enum class EvmExplorerKind {
    BLOCKSCOUT,
    ETHERSCAN,
}

data class EvmExplorerEndpoint(
    val baseUrl: String,
    val kind: EvmExplorerKind,
)
