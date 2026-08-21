package network.bahn.androidcryptowallet.ui.bitcoin.details

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionSummary
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind

data class BitcoinWalletDetailsUiState(
    val wallet: BitcoinWallet? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val transactions: List<BitcoinTransactionSummary> = emptyList(),
    val isLoadingTransactions: Boolean = true,
    val isRefreshingTransactions: Boolean = false,
    val isLoadingMoreTransactions: Boolean = false,
    val hasMoreTransactions: Boolean = false,
    val transactionsErrorMessage: String? = null,
) {
    val network: BitcoinNetwork? get() = wallet?.network
    val receiveAddress: String? get() = wallet?.receiveAddress
    val confirmedBalanceSatoshis: Long? get() = wallet?.confirmedBalanceSatoshis
    val unconfirmedBalanceSatoshis: Long? get() = wallet?.unconfirmedBalanceSatoshis
    val balanceUpdatedAtMillis: Long? get() = wallet?.balanceUpdatedAtMillis
    val isWatchOnly: Boolean get() = wallet?.kind == BitcoinWalletKind.WATCH_ONLY
}
