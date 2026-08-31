package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import network.bahn.androidcryptowallet.data.local.db.BitcoinTransactionDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinTransactionWithWalletRow
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.EvmTransactionDao
import network.bahn.androidcryptowallet.data.local.db.EvmTransactionWithWalletRow
import network.bahn.androidcryptowallet.data.local.db.EvmWalletDao
import network.bahn.androidcryptowallet.data.local.prefs.WalletNetworkModeStore
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.ConsolidatedTransaction
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode
import network.bahn.androidcryptowallet.domain.model.portfolioHeadline
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import network.bahn.androidcryptowallet.domain.repository.ConsolidatedTransactionRepository
import network.bahn.androidcryptowallet.domain.repository.EvmWalletRepository
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsolidatedTransactionRepositoryImpl @Inject constructor(
    private val bitcoinTransactionDao: BitcoinTransactionDao,
    private val evmTransactionDao: EvmTransactionDao,
    private val bitcoinWalletDao: BitcoinWalletDao,
    private val evmWalletDao: EvmWalletDao,
    private val bitcoinWalletRepository: BitcoinWalletRepository,
    private val evmWalletRepository: EvmWalletRepository,
    private val walletNetworkModeStore: WalletNetworkModeStore,
) : ConsolidatedTransactionRepository {
    override fun observeTransactions(): Flow<List<ConsolidatedTransaction>> = combine(
        bitcoinTransactionDao.observeAllWithWallet(),
        evmTransactionDao.observeAllWithWallet(),
        walletNetworkModeStore.observeMode(),
    ) { bitcoinRows, evmRows, mode ->
        mergeAndSort(
            bitcoinRows = bitcoinRows.filter { row ->
                mode.matches(BitcoinNetwork.valueOf(row.walletNetwork))
            },
            evmRows = evmRows.filter { row ->
                mode.matches(EvmNetwork.valueOf(row.walletNetwork))
            },
        )
    }

    override suspend fun refreshAllTransactions() {
        val mode = walletNetworkModeStore.observeMode().first()
        coroutineScope {
            val semaphore = Semaphore(MAX_CONCURRENT_REFRESHES)
            val bitcoinWalletIds = bitcoinWalletDao.listIdsByNetwork(mode.bitcoinNetwork().name)
            val evmWalletIds = EvmFamily.entries.flatMap { family ->
                evmWalletDao.listIdsByNetwork(mode.defaultEvmNetwork(family).name)
            }
            (bitcoinWalletIds.map { walletId ->
                async {
                    semaphore.withPermit {
                        runCatching { bitcoinWalletRepository.getTransactions(walletId) }
                    }
                }
            } + evmWalletIds.map { walletId ->
                async {
                    semaphore.withPermit {
                        runCatching { evmWalletRepository.getTransactions(walletId) }
                    }
                }
            }).awaitAll()
        }
    }

    internal companion object {
        private const val MAX_CONCURRENT_REFRESHES = 4

        fun mergeAndSort(
            bitcoinRows: List<BitcoinTransactionWithWalletRow>,
            evmRows: List<EvmTransactionWithWalletRow>,
        ): List<ConsolidatedTransaction> {
            val bitcoinTransactions = bitcoinRows.map(::mapBitcoinRow)
            val evmTransactions = evmRows.map(::mapEvmRow)
            return (bitcoinTransactions + evmTransactions).sortedWith(transactionComparator)
        }

        private val transactionComparator = Comparator<ConsolidatedTransaction> { left, right ->
            val leftTime = left.timestampSeconds
            val rightTime = right.timestampSeconds
            when {
                leftTime == null && rightTime == null -> right.id.compareTo(left.id)
                leftTime == null -> 1
                rightTime == null -> -1
                else -> {
                    val timeCompare = rightTime.compareTo(leftTime)
                    if (timeCompare != 0) timeCompare else right.id.compareTo(left.id)
                }
            }
        }

        private fun mapBitcoinRow(row: BitcoinTransactionWithWalletRow): ConsolidatedTransaction.Bitcoin {
            val network = BitcoinNetwork.valueOf(row.walletNetwork)
            return ConsolidatedTransaction.Bitcoin(
                id = "btc:${row.txid}:${row.walletId}",
                walletId = row.walletId,
                walletName = row.walletName,
                chainLabel = network.portfolioHeadline(),
                timestampSeconds = row.blockTimeSeconds,
                confirmed = row.confirmed,
                isIncoming = row.netSatoshis > 0L,
                txReference = row.txid,
                netSatoshis = row.netSatoshis,
            )
        }

        private fun mapEvmRow(row: EvmTransactionWithWalletRow): ConsolidatedTransaction.Evm {
            val network = EvmNetwork.valueOf(row.walletNetwork)
            val netWei = row.netWei.toBigIntegerOrNull() ?: BigInteger.ZERO
            return ConsolidatedTransaction.Evm(
                id = "evm:${row.hash}:${row.walletId}",
                walletId = row.walletId,
                walletName = row.walletName,
                chainLabel = network.portfolioHeadline(),
                timestampSeconds = row.blockTimeSeconds,
                confirmed = row.confirmed,
                isIncoming = netWei > BigInteger.ZERO,
                txReference = row.hash,
                netWei = row.netWei,
                nativeSymbol = network.nativeSymbol,
            )
        }
    }
}
