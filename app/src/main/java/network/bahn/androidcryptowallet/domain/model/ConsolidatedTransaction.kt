package network.bahn.androidcryptowallet.domain.model

sealed class ConsolidatedTransaction {
    abstract val id: String
    abstract val walletId: String
    abstract val walletName: String?
    abstract val chainLabel: String
    abstract val timestampSeconds: Long?
    abstract val confirmed: Boolean
    abstract val isIncoming: Boolean
    abstract val txReference: String

    data class Bitcoin(
        override val id: String,
        override val walletId: String,
        override val walletName: String?,
        override val chainLabel: String,
        override val timestampSeconds: Long?,
        override val confirmed: Boolean,
        override val isIncoming: Boolean,
        override val txReference: String,
        val netSatoshis: Long,
    ) : ConsolidatedTransaction()

    data class Evm(
        override val id: String,
        override val walletId: String,
        override val walletName: String?,
        override val chainLabel: String,
        override val timestampSeconds: Long?,
        override val confirmed: Boolean,
        override val isIncoming: Boolean,
        override val txReference: String,
        val netWei: String,
        val nativeSymbol: String,
    ) : ConsolidatedTransaction()
}
