package network.bahn.androidcryptowallet.data.remote.ms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MsTxTest {
    @Test
    fun toSummaryReceiveIsPositiveNet() {
        val tx = MsTxResponse(
            txid = "receive",
            fee = 150L,
            vout = listOf(MsTxVout(scriptpubkeyAddress = ADDRESS, value = 50_000L)),
            status = MsTxStatus(confirmed = true, blockTime = 1_700_000_000L),
        )

        val summary = tx.toSummary(ADDRESS)

        assertEquals("receive", summary.txid)
        assertTrue(summary.confirmed)
        assertEquals(1_700_000_000L, summary.blockTimeSeconds)
        assertEquals(50_000L, summary.netSatoshis)
        assertEquals(150L, summary.feeSatoshis)
    }

    @Test
    fun toSummarySendIsNegativeNet() {
        val tx = MsTxResponse(
            txid = "send",
            vin = listOf(
                MsTxVin(prevout = MsTxPrevout(scriptpubkeyAddress = ADDRESS, value = 80_000L)),
            ),
            vout = listOf(MsTxVout(scriptpubkeyAddress = "tb1qother", value = 79_000L)),
            status = MsTxStatus(confirmed = true),
        )

        assertEquals(-80_000L, tx.toSummary(ADDRESS).netSatoshis)
    }

    @Test
    fun toSummaryMixedUsesReceivedMinusSpent() {
        val tx = MsTxResponse(
            txid = "mixed",
            vin = listOf(
                MsTxVin(prevout = MsTxPrevout(scriptpubkeyAddress = ADDRESS, value = 100_000L)),
            ),
            vout = listOf(MsTxVout(scriptpubkeyAddress = ADDRESS, value = 40_000L)),
        )

        assertEquals(-60_000L, tx.toSummary(ADDRESS).netSatoshis)
    }

    @Test
    fun toSummaryMissingPrevoutCountsAsZeroSpent() {
        val tx = MsTxResponse(
            txid = "coinbase",
            vin = listOf(MsTxVin(prevout = null)),
            vout = listOf(MsTxVout(scriptpubkeyAddress = ADDRESS, value = 6_250L)),
            status = MsTxStatus(confirmed = false),
        )

        val summary = tx.toSummary(ADDRESS)
        assertEquals(6_250L, summary.netSatoshis)
        assertFalse(summary.confirmed)
        assertNull(summary.blockTimeSeconds)
    }

    @Test
    fun toTransactionPageHasMoreWhenFullAndConfirmedCursorExists() {
        val page = List(MS_TX_PAGE_SIZE) { index ->
            confirmedTx("txid-$index")
        }.toTransactionPage(ADDRESS)

        assertEquals(MS_TX_PAGE_SIZE, page.transactions.size)
        assertEquals("txid-${MS_TX_PAGE_SIZE - 1}", page.lastConfirmedTxid)
        assertTrue(page.hasMore)
    }

    @Test
    fun toTransactionPageHasNoMoreWhenShort() {
        val page = listOf(confirmedTx("txid-1"), mempoolTx("txid-0")).toTransactionPage(ADDRESS)

        assertEquals("txid-1", page.lastConfirmedTxid)
        assertFalse(page.hasMore)
    }
}

private const val ADDRESS = "tb1q6rz28mcfahecdzujk32jvf8u3vf3m48qcx3p34"

internal fun confirmedTx(txid: String, address: String = ADDRESS): MsTxResponse = MsTxResponse(
    txid = txid,
    fee = 10L,
    vout = listOf(MsTxVout(scriptpubkeyAddress = address, value = 1_000L)),
    status = MsTxStatus(confirmed = true, blockTime = 1_700_000_000L),
)

internal fun mempoolTx(txid: String, address: String = ADDRESS): MsTxResponse = MsTxResponse(
    txid = txid,
    fee = 10L,
    vout = listOf(MsTxVout(scriptpubkeyAddress = address, value = 1_000L)),
    status = MsTxStatus(confirmed = false),
)
