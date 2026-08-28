package network.bahn.androidcryptowallet.ui.ethereum.details

import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionSummary
import network.bahn.androidcryptowallet.domain.model.EthereumWallet

data class EthereumWalletDetailsUiState(
    val wallet: EthereumWallet? = null,
    val isRefreshing: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val transactions: List<EthereumTransactionSummary> = emptyList(),
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

sealed interface EthereumWalletDetailsEvent {
    data object WalletDeleted : EthereumWalletDetailsEvent
}
