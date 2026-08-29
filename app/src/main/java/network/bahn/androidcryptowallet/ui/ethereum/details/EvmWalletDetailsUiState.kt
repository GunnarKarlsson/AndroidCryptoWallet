package network.bahn.androidcryptowallet.ui.ethereum.details

import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmTransactionSummary
import network.bahn.androidcryptowallet.domain.model.EvmWallet

data class EvmWalletDetailsUiState(
    val wallet: EvmWallet? = null,
    val isRefreshing: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val transactions: List<EvmTransactionSummary> = emptyList(),
    val isLoadingTransactions: Boolean = false,
    val isRefreshingTransactions: Boolean = false,
    val isLoadingMoreTransactions: Boolean = false,
    val hasMoreTransactions: Boolean = false,
    val transactionsErrorMessage: String? = null,
) {
    val network: EvmNetwork? get() = wallet?.network
    val address: String? get() = wallet?.address
    val balanceWei: String? get() = wallet?.balanceWei
    val balanceUpdatedAtMillis: Long? get() = wallet?.balanceUpdatedAtMillis
    val family: EvmFamily? get() = wallet?.network?.family
}

sealed interface EvmWalletDetailsEvent {
    data object WalletDeleted : EvmWalletDetailsEvent
}
