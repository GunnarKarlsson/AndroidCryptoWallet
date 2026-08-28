package network.bahn.androidcryptowallet.data.remote.blockscout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPage
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EvmTransactionSummary
import java.math.BigInteger
import java.time.Instant

const val BLOCKSCOUT_TX_PAGE_SIZE = 50

@Serializable
data class BlockscoutTxPageResponse(
    val items: List<BlockscoutTxResponse> = emptyList(),
    @SerialName("next_page_params") val nextPageParams: BlockscoutNextPageParams? = null,
)

@Serializable
data class BlockscoutNextPageParams(
    @SerialName("block_number") val blockNumber: Long? = null,
    val index: Int? = null,
    val hash: String? = null,
    @SerialName("inserted_at") val insertedAt: String? = null,
    val value: String? = null,
    val fee: String? = null,
    @SerialName("items_count") val itemsCount: Int? = null,
)

@Serializable
data class BlockscoutTxResponse(
    val hash: String,
    val value: String = "0",
    val from: BlockscoutAddressRef? = null,
    val to: BlockscoutAddressRef? = null,
    val timestamp: String? = null,
    @SerialName("block_number") val blockNumber: Long? = null,
    val status: String? = null,
    val fee: BlockscoutFee? = null,
)

@Serializable
data class BlockscoutAddressRef(
    val hash: String? = null,
)

@Serializable
data class BlockscoutFee(
    val value: String? = null,
)

fun BlockscoutTxPageResponse.toTransactionPage(address: String): EvmTransactionPage {
    val normalizedAddress = address.lowercase()
    val transactions = items.map { it.toSummary(normalizedAddress) }
    val nextCursor = nextPageParams?.toCursor()
    return EvmTransactionPage(
        transactions = transactions,
        nextCursor = nextCursor,
        hasMore = items.size == BLOCKSCOUT_TX_PAGE_SIZE && nextCursor != null,
    )
}

fun BlockscoutNextPageParams.toCursor(): EvmTransactionPaginationCursor =
    EvmTransactionPaginationCursor(
        blockNumber = blockNumber,
        index = index,
        hash = hash,
        insertedAt = insertedAt,
        value = value,
        fee = fee,
        itemsCount = itemsCount,
    )

fun BlockscoutTxResponse.toSummary(normalizedAddress: String): EvmTransactionSummary {
    val fromAddress = from?.hash?.lowercase()
    val toAddress = to?.hash?.lowercase()
    val valueWei = value.toBigIntegerOrZero()
    val netWei = when {
        status == "error" -> BigInteger.ZERO
        toAddress == normalizedAddress && fromAddress != normalizedAddress -> valueWei
        fromAddress == normalizedAddress && toAddress != normalizedAddress -> valueWei.negate()
        else -> BigInteger.ZERO
    }
    return EvmTransactionSummary(
        hash = hash,
        confirmed = blockNumber != null,
        blockTimeSeconds = timestamp?.let(::parseIsoTimestampSeconds),
        netWei = netWei.toString(),
        feeWei = fee?.value,
    )
}

private fun String.toBigIntegerOrZero(): BigInteger = runCatching { BigInteger(this) }
    .getOrDefault(BigInteger.ZERO)

internal fun parseIsoTimestampSeconds(iso: String): Long? = runCatching {
    Instant.parse(iso).epochSecond
}.getOrNull()
