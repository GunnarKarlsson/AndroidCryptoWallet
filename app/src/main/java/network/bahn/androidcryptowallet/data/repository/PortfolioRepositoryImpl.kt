package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmWallet
import network.bahn.androidcryptowallet.domain.model.PortfolioHolding
import network.bahn.androidcryptowallet.domain.model.PortfolioHoldingDestination
import network.bahn.androidcryptowallet.domain.model.portfolioHeadline
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository
import network.bahn.androidcryptowallet.domain.repository.PortfolioRepository
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val bitcoinWalletRepository: BitcoinWalletRepository,
    private val evmWalletRepository: EvmWalletRepository,
) : PortfolioRepository {
    override fun observeHoldings(): Flow<List<PortfolioHolding>> {
        val evmFlows = EvmFamily.entries.map { family ->
            evmWalletRepository.observeWallets(family).map { wallets ->
                aggregateEvm(family, wallets)?.let(::listOf) ?: emptyList()
            }
        }
        return combine(
            bitcoinWalletRepository.observeWallets().map(::aggregateBitcoin),
            combine(evmFlows) { arrays ->
                arrays.flatMap { holdings -> holdings as List<PortfolioHolding> }
            },
        ) { bitcoinHoldings, evmHoldings ->
            (bitcoinHoldings + evmHoldings).sortedBy { it.headline.lowercase() }
        }
    }

    override suspend fun refreshAllBalances() {
        coroutineScope {
            val semaphore = Semaphore(MAX_CONCURRENT_REFRESHES)
            val bitcoinWallets = bitcoinWalletRepository.observeWallets().first()
            val evmWallets = EvmFamily.entries.flatMap { family ->
                evmWalletRepository.observeWallets(family).first()
            }
            (bitcoinWallets.map { wallet ->
                async {
                    semaphore.withPermit {
                        bitcoinWalletRepository.refreshBalance(wallet.id)
                    }
                }
            } + evmWallets.map { wallet ->
                async {
                    semaphore.withPermit {
                        evmWalletRepository.refreshBalance(wallet.id)
                    }
                }
            }).awaitAll()
        }
    }

    internal companion object {
        private const val MAX_CONCURRENT_REFRESHES = 4

        fun aggregateBitcoin(wallets: List<BitcoinWallet>): List<PortfolioHolding> {
            val totalSatoshis = wallets.sumOf { wallet ->
                (wallet.confirmedBalanceSatoshis ?: 0L) + (wallet.unconfirmedBalanceSatoshis ?: 0L)
            }
            if (totalSatoshis == 0L) return emptyList()
            return listOf(
                PortfolioHolding(
                    destination = PortfolioHoldingDestination.Bitcoin,
                    headline = wallets.first().network.portfolioHeadline(),
                    nativeSymbol = "BTC",
                    balanceSatoshis = totalSatoshis,
                ),
            )
        }

        fun aggregateEvm(family: EvmFamily, wallets: List<EvmWallet>): PortfolioHolding? {
            if (wallets.isEmpty()) return null
            val network = wallets.first().network
            val totalWei = wallets.fold(BigInteger.ZERO) { acc, wallet ->
                acc + (wallet.balanceWei?.toBigIntegerOrNull() ?: BigInteger.ZERO)
            }
            if (totalWei == BigInteger.ZERO) return null
            return PortfolioHolding(
                destination = PortfolioHoldingDestination.Evm(family),
                headline = network.portfolioHeadline(),
                nativeSymbol = network.nativeSymbol,
                balanceWei = totalWei,
            )
        }
    }
}
