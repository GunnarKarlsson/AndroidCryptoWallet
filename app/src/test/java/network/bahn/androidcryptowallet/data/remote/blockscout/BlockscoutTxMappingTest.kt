package network.bahn.androidcryptowallet.data.remote.blockscout

import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class BlockscoutTxMappingTest {
    private val wallet = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94"

    @Test
    fun incomingTransferIsPositiveNetWei() {
        val summary = BlockscoutTxResponse(
            hash = "0x1",
            value = "1000000000000000000",
            from = BlockscoutAddressRef(hash = "0xA"),
            to = BlockscoutAddressRef(hash = wallet),
            timestamp = "2026-08-24T23:23:36.000000Z",
            blockNumber = 1L,
            status = "ok",
            fee = BlockscoutFee(value = "21000000000000"),
        ).toSummary(wallet.lowercase())

        assertEquals("1000000000000000000", summary.netWei)
        assertTrue(summary.confirmed)
        assertEquals(1_787_613_816L, summary.blockTimeSeconds)
    }

    @Test
    fun outgoingTransferIsNegativeNetWei() {
        val summary = BlockscoutTxResponse(
            hash = "0x2",
            value = "500000000000000000",
            from = BlockscoutAddressRef(hash = wallet),
            to = BlockscoutAddressRef(hash = "0xB"),
            blockNumber = 2L,
            status = "ok",
        ).toSummary(wallet.lowercase())

        assertEquals("-500000000000000000", summary.netWei)
    }

    @Test
    fun failedTransferHasZeroNetWei() {
        val summary = BlockscoutTxResponse(
            hash = "0x3",
            value = "1000000000000000000",
            from = BlockscoutAddressRef(hash = "0xA"),
            to = BlockscoutAddressRef(hash = wallet),
            blockNumber = 3L,
            status = "error",
        ).toSummary(wallet.lowercase())

        assertEquals(BigInteger.ZERO.toString(), summary.netWei)
    }

    @Test
    fun pageHasMoreWhenFullPageAndCursorPresent() {
        val items = List(BLOCKSCOUT_TX_PAGE_SIZE) { index ->
            BlockscoutTxResponse(
                hash = "0x$index",
                value = "1",
                from = BlockscoutAddressRef(hash = "0xA"),
                to = BlockscoutAddressRef(hash = wallet),
                blockNumber = index.toLong(),
                status = "ok",
            )
        }
        val page = BlockscoutTxPageResponse(
            items = items,
            nextPageParams = BlockscoutNextPageParams(
                blockNumber = 99L,
                index = 1,
                hash = "0xlast",
                itemsCount = 50,
            ),
        ).toTransactionPage(wallet)

        assertTrue(page.hasMore)
        assertEquals(
            EthereumTransactionPaginationCursor(
                blockNumber = 99L,
                index = 1,
                hash = "0xlast",
                insertedAt = null,
                value = null,
                fee = null,
                itemsCount = 50,
            ),
            page.nextCursor,
        )
    }

    @Test
    fun pageHasNoMoreWhenCursorMissing() {
        val page = BlockscoutTxPageResponse(
            items = listOf(
                BlockscoutTxResponse(
                    hash = "0x1",
                    from = BlockscoutAddressRef(hash = "0xA"),
                    to = BlockscoutAddressRef(hash = wallet),
                    blockNumber = 1L,
                ),
            ),
            nextPageParams = null,
        ).toTransactionPage(wallet)

        assertFalse(page.hasMore)
        assertNull(page.nextCursor)
    }

    @Test
    fun cursorRoundTripsQueryParams() {
        val cursor = EthereumTransactionPaginationCursor(
            blockNumber = 11560237L,
            index = 175,
            hash = "0xabc",
            insertedAt = "2026-07-06T20:08:40.809624Z",
            value = "10000000000000000",
            fee = "23619632616000",
            itemsCount = 50,
        )

        assertEquals(
            mapOf(
                "block_number" to "11560237",
                "index" to "175",
                "hash" to "0xabc",
                "inserted_at" to "2026-07-06T20:08:40.809624Z",
                "value" to "10000000000000000",
                "fee" to "23619632616000",
                "items_count" to "50",
            ),
            cursor.toQueryParams(),
        )
    }
}
