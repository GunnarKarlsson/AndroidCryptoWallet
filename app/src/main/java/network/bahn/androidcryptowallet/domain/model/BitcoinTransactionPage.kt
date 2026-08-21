package network.bahn.androidcryptowallet.domain.model

data class BitcoinTransactionPage(
    val transactions: List<BitcoinTransactionSummary>,
    val lastConfirmedTxid: String?,
    val hasMore: Boolean,
)
