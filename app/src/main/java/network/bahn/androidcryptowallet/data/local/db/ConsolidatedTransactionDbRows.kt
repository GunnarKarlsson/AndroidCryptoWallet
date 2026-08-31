package network.bahn.androidcryptowallet.data.local.db

data class BitcoinTransactionWithWalletRow(
    val walletId: String,
    val txid: String,
    val confirmed: Boolean,
    val blockTimeSeconds: Long?,
    val netSatoshis: Long,
    val feeSatoshis: Long?,
    val sortIndex: Int,
    val walletName: String?,
    val walletNetwork: String,
)

data class EvmTransactionWithWalletRow(
    val walletId: String,
    val hash: String,
    val confirmed: Boolean,
    val blockTimeSeconds: Long?,
    val netWei: String,
    val feeWei: String?,
    val sortIndex: Int,
    val walletName: String?,
    val walletNetwork: String,
)
