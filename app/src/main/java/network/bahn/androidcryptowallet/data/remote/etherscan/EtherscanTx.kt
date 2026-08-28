package network.bahn.androidcryptowallet.data.remote.etherscan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPage
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EvmTransactionSummary
import java.math.BigInteger

const val ETHERSCAN_TX_PAGE_SIZE = 50

@Serializable
data class EtherscanTxResponse(
    val hash: String = "",
    val from: String = "",
    val to: String = "",
    val value: String = "0",
    @SerialName("timeStamp") val timeStamp: String? = null,
    @SerialName("blockNumber") val blockNumber: String? = null,
    @SerialName("isError") val isError: String? = null,
    @SerialName("txreceipt_status") val txReceiptStatus: String? = null,
    @SerialName("gasUsed") val gasUsed: String? = null,
    @SerialName("gasPrice") val gasPrice: String? = null,
)

fun parseEtherscanTxList(body: String, json: Json): List<EtherscanTxResponse> {
    val root = json.parseToJsonElement(body)
    val objectBody = root as? JsonObject ?: error("Etherscan response is not a JSON object")
    val status = objectBody.stringField("status") ?: "0"
    val message = objectBody.stringField("message").orEmpty()
    if (status != "1") {
        if (message.contains("No transactions found", ignoreCase = true)) {
            return emptyList()
        }
        error("Etherscan API error: $message")
    }
    return when (val result = objectBody["result"]) {
        is JsonArray -> json.decodeFromJsonElement(result)
        is JsonPrimitive -> emptyList()
        null -> emptyList()
        else -> emptyList()
    }
}

fun List<EtherscanTxResponse>.toTransactionPage(
    address: String,
    page: Int,
): EvmTransactionPage {
    val normalizedAddress = address.lowercase()
    val transactions = map { it.toSummary(normalizedAddress) }
    val hasMore = size == ETHERSCAN_TX_PAGE_SIZE
    return EvmTransactionPage(
        transactions = transactions,
        nextCursor = if (hasMore) {
            EvmTransactionPaginationCursor(
                blockNumber = null,
                index = null,
                hash = null,
                insertedAt = null,
                value = null,
                fee = null,
                page = page + 1,
            )
        } else {
            null
        },
        hasMore = hasMore,
    )
}

fun EtherscanTxResponse.toSummary(normalizedAddress: String): EvmTransactionSummary {
    val fromAddress = from.lowercase()
    val toAddress = to.lowercase()
    val valueWei = value.toBigIntegerOrZero()
    val failed = isError == "1" || txReceiptStatus == "0"
    val netWei = when {
        failed -> BigInteger.ZERO
        toAddress == normalizedAddress && fromAddress != normalizedAddress -> valueWei
        fromAddress == normalizedAddress && toAddress != normalizedAddress -> valueWei.negate()
        else -> BigInteger.ZERO
    }
    val feeWei = gasUsed?.toBigIntegerOrNull()?.let { used ->
        gasPrice?.toBigIntegerOrNull()?.multiply(used)
    }
    return EvmTransactionSummary(
        hash = hash,
        confirmed = blockNumber != null && !failed,
        blockTimeSeconds = timeStamp?.toLongOrNull(),
        netWei = netWei.toString(),
        feeWei = feeWei?.toString(),
    )
}

private fun JsonObject.stringField(key: String): String? =
    (this[key] as? JsonPrimitive)?.content

private fun String.toBigIntegerOrZero(): BigInteger = runCatching { BigInteger(this) }
    .getOrDefault(BigInteger.ZERO)
