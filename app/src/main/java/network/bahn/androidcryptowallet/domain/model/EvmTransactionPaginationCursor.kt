package network.bahn.androidcryptowallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EvmTransactionPaginationCursor(
    val blockNumber: Long?,
    val index: Int?,
    val hash: String?,
    val insertedAt: String?,
    val value: String?,
    val fee: String?,
    val itemsCount: Int? = null,
    /** Etherscan-compatible explorers use 1-based page numbers. */
    val page: Int? = null,
) {
    fun toQueryParams(): Map<String, String> = buildMap {
        blockNumber?.let { put("block_number", it.toString()) }
        index?.let { put("index", it.toString()) }
        hash?.let { put("hash", it) }
        insertedAt?.let { put("inserted_at", it) }
        value?.let { put("value", it) }
        fee?.let { put("fee", it) }
        itemsCount?.let { put("items_count", it.toString()) }
    }
}
