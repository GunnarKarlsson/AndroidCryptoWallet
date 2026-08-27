package network.bahn.androidcryptowallet.ui.ethereum.edit

data class EthereumEditWalletUiState(
    val name: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isWalletLoaded: Boolean = false,
) {
    val canConfirm: Boolean
        get() = isWalletLoaded && !isSubmitting
}
