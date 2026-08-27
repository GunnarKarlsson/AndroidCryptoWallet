package network.bahn.androidcryptowallet.ui.ethereum.details

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet

data class EthereumWalletDetailsUiState(
    val wallet: EthereumWallet? = null,
    val isRefreshing: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
) {
    val network: EthereumNetwork? get() = wallet?.network
    val address: String? get() = wallet?.address
    val balanceWei: String? get() = wallet?.balanceWei
    val balanceUpdatedAtMillis: Long? get() = wallet?.balanceUpdatedAtMillis
}

sealed interface EthereumWalletDetailsEvent {
    data object WalletDeleted : EthereumWalletDetailsEvent
}
