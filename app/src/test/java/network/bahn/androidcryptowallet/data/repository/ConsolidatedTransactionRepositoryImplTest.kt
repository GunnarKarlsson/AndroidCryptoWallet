package network.bahn.androidcryptowallet.data.repository

import network.bahn.androidcryptowallet.data.local.db.BitcoinTransactionWithWalletRow
import network.bahn.androidcryptowallet.data.local.db.EvmTransactionWithWalletRow
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.ConsolidatedTransaction
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.portfolioHeadline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsolidatedTransactionRepositoryImplTest {
    @Test
    fun mergeAndSort_ordersByTimestampDescendingWithNullsLast() {
        val merged = ConsolidatedTransactionRepositoryImpl.mergeAndSort(
            bitcoinRows = listOf(
                bitcoinRow(
                    txid = "btc-old",
                    blockTimeSeconds = 100L,
                    walletId = "btc-wallet",
                ),
                bitcoinRow(
                    txid = "btc-null-time",
                    blockTimeSeconds = null,
                    walletId = "btc-wallet",
                ),
            ),
            evmRows = listOf(
                evmRow(
                    hash = "evm-new",
                    blockTimeSeconds = 200L,
                    walletId = "evm-wallet",
                    network = EvmNetwork.SEPOLIA.name,
                ),
            ),
        )

        assertEquals(3, merged.size)
        assertEquals("evm:evm-new:evm-wallet", merged[0].id)
        assertEquals("btc:btc-old:btc-wallet", merged[1].id)
        assertEquals("btc:btc-null-time:btc-wallet", merged[2].id)
        assertEquals(null, merged[2].timestampSeconds)
    }

    @Test
    fun mergeAndSort_mapsBitcoinAndEvmFieldsWithNetworkLabels() {
        val merged = ConsolidatedTransactionRepositoryImpl.mergeAndSort(
            bitcoinRows = listOf(
                bitcoinRow(
                    txid = "abc",
                    blockTimeSeconds = 1_700_000_000L,
                    netSatoshis = 50_000L,
                    walletName = "Savings",
                    walletId = "wallet-btc",
                    network = BitcoinNetwork.TESTNET4.name,
                ),
            ),
            evmRows = listOf(
                evmRow(
                    hash = "0xdef",
                    blockTimeSeconds = 1_700_000_001L,
                    netWei = "-1000000000000000000",
                    walletName = null,
                    walletId = "wallet-evm",
                    network = EvmNetwork.ARBITRUM_MAINNET.name,
                ),
            ),
        )

        val bitcoin = merged.single { it is ConsolidatedTransaction.Bitcoin } as ConsolidatedTransaction.Bitcoin
        assertEquals("wallet-btc", bitcoin.walletId)
        assertEquals("Savings", bitcoin.walletName)
        assertEquals(BitcoinNetwork.TESTNET4.portfolioHeadline(), bitcoin.chainLabel)
        assertTrue(bitcoin.isIncoming)
        assertEquals("abc", bitcoin.txReference)

        val evm = merged.single { it is ConsolidatedTransaction.Evm } as ConsolidatedTransaction.Evm
        assertEquals("wallet-evm", evm.walletId)
        assertEquals(EvmNetwork.ARBITRUM_MAINNET.portfolioHeadline(), evm.chainLabel)
        assertEquals("ETH", evm.nativeSymbol)
        assertFalse(evm.isIncoming)
        assertEquals("0xdef", evm.txReference)
    }

    private fun bitcoinRow(
        txid: String,
        blockTimeSeconds: Long?,
        walletId: String,
        netSatoshis: Long = 100L,
        walletName: String? = "Wallet",
        network: String = BitcoinNetwork.MAINNET.name,
    ) = BitcoinTransactionWithWalletRow(
        walletId = walletId,
        txid = txid,
        confirmed = blockTimeSeconds != null,
        blockTimeSeconds = blockTimeSeconds,
        netSatoshis = netSatoshis,
        feeSatoshis = null,
        sortIndex = 0,
        walletName = walletName,
        walletNetwork = network,
    )

    private fun evmRow(
        hash: String,
        blockTimeSeconds: Long?,
        walletId: String,
        netWei: String = "1000000000000000000",
        walletName: String? = "Wallet",
        network: String,
    ) = EvmTransactionWithWalletRow(
        walletId = walletId,
        hash = hash,
        confirmed = blockTimeSeconds != null,
        blockTimeSeconds = blockTimeSeconds,
        netWei = netWei,
        feeWei = null,
        sortIndex = 0,
        walletName = walletName,
        walletNetwork = network,
    )
}
