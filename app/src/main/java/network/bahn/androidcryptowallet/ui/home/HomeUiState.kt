package network.bahn.androidcryptowallet.ui.home

enum class BitcoinNetwork(val label: String) {
    TESTNET4("Testnet4"),
    MAINNET("Mainnet"),
}

data class HomeUiState(
    val selectedNetwork: BitcoinNetwork = BitcoinNetwork.TESTNET4,
    val blockHeight: Long? = null,
    val updatedAtMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
