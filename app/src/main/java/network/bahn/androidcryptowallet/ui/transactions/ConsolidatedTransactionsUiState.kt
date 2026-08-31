package network.bahn.androidcryptowallet.ui.transactions

import network.bahn.androidcryptowallet.domain.model.ConsolidatedTransaction
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode

data class ConsolidatedTransactionsUiState(
    val transactions: List<ConsolidatedTransaction> = emptyList(),
    val networkMode: WalletNetworkMode = WalletNetworkMode.TESTNET,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)
