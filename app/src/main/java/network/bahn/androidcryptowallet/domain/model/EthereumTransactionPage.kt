package network.bahn.androidcryptowallet.domain.model

data class EthereumTransactionPage(
    val transactions: List<EthereumTransactionSummary>,
    val nextCursor: EthereumTransactionPaginationCursor?,
    val hasMore: Boolean,
)
