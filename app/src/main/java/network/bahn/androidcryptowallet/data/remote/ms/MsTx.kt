package network.bahn.androidcryptowallet.data.remote.ms

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionSummary

const val MS_TX_PAGE_SIZE = 25

@Serializable
data class MsTxResponse(
    val txid: String,
    val fee: Long? = null,
    val vin: List<MsTxVin> = emptyList(),
    val vout: List<MsTxVout> = emptyList(),
    val status: MsTxStatus? = null,
)

@Serializable
data class MsTxVin(
    val prevout: MsTxPrevout? = null,
)

@Serializable
data class MsTxPrevout(
    @SerialName("scriptpubkey_address") val scriptpubkeyAddress: String? = null,
    val value: Long = 0L,
)

@Serializable
data class MsTxVout(
    @SerialName("scriptpubkey_address") val scriptpubkeyAddress: String? = null,
    val value: Long = 0L,
)

@Serializable
data class MsTxStatus(
    val confirmed: Boolean = false,
    @SerialName("block_time") val blockTime: Long? = null,
)

fun List<MsTxResponse>.toTransactionPage(address: String): BitcoinTransactionPage {
    val transactions = map { it.toSummary(address) }
    val lastConfirmedTxid = transactions.lastOrNull { it.confirmed }?.txid
    return BitcoinTransactionPage(
        transactions = transactions,
        lastConfirmedTxid = lastConfirmedTxid,
        hasMore = transactions.size == MS_TX_PAGE_SIZE && lastConfirmedTxid != null,
    )
}

fun MsTxResponse.toSummary(address: String): BitcoinTransactionSummary {
    val received = vout
        .filter { it.scriptpubkeyAddress == address }
        .sumOf { it.value }
    val spent = vin.mapNotNull { it.prevout }
        .filter { it.scriptpubkeyAddress == address }
        .sumOf { it.value }
    return BitcoinTransactionSummary(
        txid = txid,
        confirmed = status?.confirmed == true,
        blockTimeSeconds = status?.blockTime,
        netSatoshis = received - spent,
        feeSatoshis = fee,
    )
}
