package network.bahn.androidcryptowallet.ui.bitcoin.edit

data class BitcoinEditWalletUiState(
    val name: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isWalletLoaded: Boolean = false,
) {
    val canConfirm: Boolean
        get() = isWalletLoaded && !isSubmitting
}
