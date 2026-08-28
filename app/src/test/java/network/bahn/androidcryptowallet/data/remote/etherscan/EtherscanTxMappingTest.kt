package network.bahn.androidcryptowallet.data.remote.etherscan

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class EtherscanTxMappingTest {
    private val wallet = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94"
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun incomingTransferIsPositiveNetWei() {
        val summary = EtherscanTxResponse(
            hash = "0x1",
            value = "1000000000000000000",
            from = "0xA",
            to = wallet,
            timeStamp = "1787613816",
            blockNumber = "1",
            isError = "0",
            gasUsed = "21000",
            gasPrice = "1000000000",
        ).toSummary(wallet.lowercase())

        assertEquals("1000000000000000000", summary.netWei)
        assertTrue(summary.confirmed)
        assertEquals(1_787_613_816L, summary.blockTimeSeconds)
        assertEquals("21000000000000", summary.feeWei)
    }

    @Test
    fun outgoingTransferIsNegativeNetWei() {
        val summary = EtherscanTxResponse(
            hash = "0x2",
            value = "500000000000000000",
            from = wallet,
            to = "0xB",
            blockNumber = "2",
            isError = "0",
        ).toSummary(wallet.lowercase())

        assertEquals("-500000000000000000", summary.netWei)
    }

    @Test
    fun failedTransferHasZeroNetWei() {
        val summary = EtherscanTxResponse(
            hash = "0x3",
            value = "1000000000000000000",
            from = "0xA",
            to = wallet,
            blockNumber = "3",
            isError = "1",
        ).toSummary(wallet.lowercase())

        assertEquals(BigInteger.ZERO.toString(), summary.netWei)
        assertFalse(summary.confirmed)
    }

    @Test
    fun parseEtherscanTxList_returnsEmptyForNoTransactionsMessage() {
        val body = """
            {"status":"0","message":"No transactions found","result":[]}
        """.trimIndent()

        assertTrue(parseEtherscanTxList(body, json).isEmpty())
    }

    @Test
    fun pageHasMoreWhenFullPageReturned() {
        val items = List(ETHERSCAN_TX_PAGE_SIZE) { index ->
            EtherscanTxResponse(
                hash = "0x$index",
                value = "1",
                from = "0xA",
                to = wallet,
                blockNumber = index.toString(),
                isError = "0",
            )
        }
        val page = items.toTransactionPage(wallet, page = 1)

        assertTrue(page.hasMore)
        assertEquals(2, page.nextCursor?.page)
    }

    @Test
    fun pageHasNoMoreWhenPartialPageReturned() {
        val page = listOf(
            EtherscanTxResponse(
                hash = "0x1",
                from = "0xA",
                to = wallet,
                blockNumber = "1",
                isError = "0",
            ),
        ).toTransactionPage(wallet, page = 1)

        assertFalse(page.hasMore)
        assertEquals(null, page.nextCursor)
    }
}
