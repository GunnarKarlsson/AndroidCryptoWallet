package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.data.local.db.EthereumTransactionDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletTxCacheEntity
import network.bahn.androidcryptowallet.data.local.db.nextCursor
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.data.local.db.toEntity
import network.bahn.androidcryptowallet.data.local.db.toJson
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.data.remote.EthereumRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.blockscout.EthereumTransactionRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.EthereumKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPage
import network.bahn.androidcryptowallet.domain.model.EthereumTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EthereumWalletRepositoryImpl @Inject constructor(
    private val keyEngine: EthereumKeyEngine,
    private val mnemonicStore: EthereumMnemonicStore,
    private val walletDao: EthereumWalletDao,
    private val transactionDao: EthereumTransactionDao,
    private val selectedEthereumNetworkStore: SelectedEthereumNetworkStore,
    private val remote: EthereumRemoteDataSource,
    private val transactionRemote: EthereumTransactionRemoteDataSource,
    private val timeProvider: TimeProvider,
    private val json: Json,
) : EthereumWalletRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeWallets(): Flow<List<EthereumWallet>> =
        selectedEthereumNetworkStore.selectedNetwork.flatMapLatest { network ->
            walletDao.observeByNetwork(network.name).map { rows -> rows.map { it.toDomain() } }
        }

    override fun observeWallet(id: String): Flow<EthereumWallet?> =
        walletDao.observeById(id).map { it?.toDomain() }

    override fun generateMnemonic(): List<String> = keyEngine.generateMnemonic()

    override suspend fun createWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) {
        keyEngine.validateMnemonic(mnemonicWords)
        val derived = keyEngine.deriveReceiveAddress(mnemonicWords, passphrase)
        val wallet = EthereumWallet(
            id = UUID.randomUUID().toString(),
            network = network,
            address = derived.address,
            derivationIndex = derived.index,
        )
        mnemonicStore.save(
            walletId = wallet.id,
            mnemonic = mnemonicWords.joinToString(" "),
            passphrase = passphrase?.takeIf { it.isNotEmpty() },
            network = network,
        )
        walletDao.insert(wallet.toEntity())
    }

    override suspend fun restoreWallet(
        network: EthereumNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) {
        keyEngine.validateMnemonic(mnemonicWords)
        val derived = keyEngine.deriveReceiveAddress(mnemonicWords, passphrase)
        val existing = walletDao.findByNetworkAndAddress(network.name, derived.address)
        if (existing != null) return
        createWallet(network, mnemonicWords, passphrase)
    }

    override suspend fun refreshBalance(walletId: String) {
        val wallet = walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        val balance = remote.getAddressBalance(
            network = EthereumNetwork.valueOf(wallet.network),
            address = wallet.address,
        )
        walletDao.updateBalance(
            id = walletId,
            balanceWei = balance.balanceWei,
            updatedAtMillis = timeProvider.nowMillis(),
        )
    }

    override suspend fun deleteWallet(walletId: String) {
        mnemonicStore.delete(walletId)
        walletDao.deleteById(walletId)
    }

    override suspend fun renameWallet(walletId: String, name: String?) {
        walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        val trimmed = name?.trim()?.takeIf { it.isNotEmpty() }
        walletDao.updateName(walletId, trimmed)
    }

    override suspend fun getCachedTransactions(walletId: String): EthereumTransactionPage? {
        val cache = transactionDao.cacheForWallet(walletId) ?: return null
        val transactions = transactionDao.listByWalletId(walletId).map { it.toDomain() }
        return EthereumTransactionPage(
            transactions = transactions,
            nextCursor = cache.nextCursor(json),
            hasMore = cache.hasMore,
        )
    }

    override suspend fun getTransactions(
        walletId: String,
        afterCursor: EthereumTransactionPaginationCursor?,
    ): EthereumTransactionPage {
        val wallet = walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        val page = transactionRemote.getAddressTransactions(
            network = EthereumNetwork.valueOf(wallet.network),
            address = wallet.address,
            afterCursor = afterCursor,
        )
        persistTransactions(walletId, page, replace = afterCursor == null)
        return page
    }

    private suspend fun persistTransactions(
        walletId: String,
        page: EthereumTransactionPage,
        replace: Boolean,
    ) {
        val startIndex = if (replace) 0 else transactionDao.maxSortIndex(walletId) + 1
        val entities = page.transactions.mapIndexed { index, tx ->
            tx.toEntity(walletId = walletId, sortIndex = startIndex + index)
        }
        val cache = EthereumWalletTxCacheEntity(
            walletId = walletId,
            nextCursorJson = page.nextCursor?.toJson(json),
            hasMore = page.hasMore,
            fetchedAtMillis = timeProvider.nowMillis(),
        )
        if (replace) {
            transactionDao.replaceWalletTransactions(walletId, entities, cache)
        } else {
            transactionDao.appendWalletTransactions(entities, cache)
        }
    }
}
