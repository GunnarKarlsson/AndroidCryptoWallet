package network.bahn.androidcryptowallet.ui.ethereum.edit

import network.bahn.androidcryptowallet.domain.model.EvmFamily

data class EthereumEditWalletUiState(
    val family: EvmFamily? = null,
    val name: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isWalletLoaded: Boolean = false,
) {
    val canConfirm: Boolean
        get() = isWalletLoaded && !isSubmitting
}
