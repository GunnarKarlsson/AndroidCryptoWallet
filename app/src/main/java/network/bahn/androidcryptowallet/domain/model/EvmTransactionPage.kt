package network.bahn.androidcryptowallet.domain.model

data class EvmTransactionPage(
    val transactions: List<EvmTransactionSummary>,
    val nextCursor: EvmTransactionPaginationCursor?,
    val hasMore: Boolean,
)
