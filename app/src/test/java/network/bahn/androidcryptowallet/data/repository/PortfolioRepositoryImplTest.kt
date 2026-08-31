package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmWallet
import network.bahn.androidcryptowallet.domain.model.PortfolioHoldingDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class PortfolioRepositoryImplTest {
    @Test
    fun aggregateBitcoin_sumsConfirmedAndUnconfirmed_andHidesZero() {
        val wallets = listOf(
            wallet(confirmed = 100L, unconfirmed = 50L),
            wallet(confirmed = 200L, unconfirmed = null),
        )
        val holdings = PortfolioRepositoryImpl.aggregateBitcoin(wallets)
        assertEquals(1, holdings.size)
        assertEquals(350L, holdings.single().balanceSatoshis)
        assertEquals(PortfolioHoldingDestination.Bitcoin, holdings.single().destination)

        assertTrue(PortfolioRepositoryImpl.aggregateBitcoin(emptyList()).isEmpty())
        assertTrue(
            PortfolioRepositoryImpl.aggregateBitcoin(
                listOf(wallet(confirmed = 0L, unconfirmed = 0L)),
            ).isEmpty(),
        )
    }

    @Test
    fun aggregateEvm_sumsWei_andHidesZero() {
        val wallets = listOf(
            evmWallet(balanceWei = "1000000000000000000"),
            evmWallet(balanceWei = "2000000000000000000"),
        )
        val holding = PortfolioRepositoryImpl.aggregateEvm(EvmFamily.ETHEREUM, wallets)
        requireNotNull(holding)
        assertEquals(BigInteger("3000000000000000000"), holding.balanceWei)
        assertEquals("ETH", holding.nativeSymbol)
        assertEquals("Ethereum Sepolia (ETH)", holding.headline)

        assertEquals(null, PortfolioRepositoryImpl.aggregateEvm(EvmFamily.BSC, emptyList()))
        assertEquals(
            null,
            PortfolioRepositoryImpl.aggregateEvm(
                EvmFamily.BSC,
                listOf(evmWallet(network = EvmNetwork.BSC_MAINNET, balanceWei = "0")),
            ),
        )
    }

    @Test
    fun observeHoldings_sortsAlphabeticallyAndFiltersZeroBalances() = runTest {
        val repo = PortfolioRepositoryImpl(
            bitcoinWalletRepository = FakeBitcoinWalletRepository(
                wallets = listOf(wallet(confirmed = 100_000L)),
            ),
            evmWalletRepository = FakeEvmWalletRepository(
                walletsByFamily = mapOf(
                    EvmFamily.POLYGON to listOf(
                        evmWallet(
                            network = EvmNetwork.POLYGON_MAINNET,
                            balanceWei = "1000000000000000000",
                        ),
                    ),
                    EvmFamily.ETHEREUM to listOf(
                        evmWallet(balanceWei = "0"),
                    ),
                ),
            ),
        )

        val holdings = repo.observeHoldings().first()
        assertEquals(2, holdings.size)
        assertEquals("Bitcoin Testnet4 (BTC)", holdings[0].headline)
        assertEquals("Polygon Mainnet (POL)", holdings[1].headline)
    }

    private fun wallet(
        confirmed: Long?,
        unconfirmed: Long? = null,
    ) = BitcoinWallet(
        id = "btc-1",
        network = BitcoinNetwork.TESTNET4,
        receiveAddress = "tb1qtest",
        confirmedBalanceSatoshis = confirmed,
        unconfirmedBalanceSatoshis = unconfirmed,
    )

    private fun evmWallet(
        network: EvmNetwork = EvmNetwork.SEPOLIA,
        balanceWei: String?,
    ) = EvmWallet(
        id = "evm-${network.name}",
        network = network,
        address = "0xabc",
        balanceWei = balanceWei,
    )
}
