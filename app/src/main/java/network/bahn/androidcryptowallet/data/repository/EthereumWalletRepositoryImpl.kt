package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import network.bahn.androidcryptowallet.data.local.db.EthereumTransactionDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletTxCacheEntity
import network.bahn.androidcryptowallet.data.local.db.nextCursor
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.data.local.db.toEntity
import network.bahn.androidcryptowallet.data.local.db.toJson
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEvmNetworkStore
import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.data.remote.EthereumRemoteDataSource
import network.bahn.androidcryptowallet.data.remote.blockscout.EthereumTransactionRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.EthereumKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.EvmFeeData
import network.bahn.androidcryptowallet.domain.model.EvmGasPreset
import network.bahn.androidcryptowallet.domain.model.EvmGasQuotes
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPage
import network.bahn.androidcryptowallet.domain.model.EvmTransactionPaginationCursor
import network.bahn.androidcryptowallet.domain.model.EthereumWallet
import network.bahn.androidcryptowallet.domain.repository.EthereumWalletRepository
import java.math.BigInteger
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EthereumWalletRepositoryImpl @Inject constructor(
    private val keyEngine: EthereumKeyEngine,
    private val mnemonicStore: EthereumMnemonicStore,
    private val walletDao: EthereumWalletDao,
    private val transactionDao: EthereumTransactionDao,
    private val selectedEvmNetworkStore: SelectedEvmNetworkStore,
    private val remote: EthereumRemoteDataSource,
    private val transactionRemote: EthereumTransactionRemoteDataSource,
    private val timeProvider: TimeProvider,
    private val json: Json,
) : EthereumWalletRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeWallets(family: EvmFamily): Flow<List<EthereumWallet>> =
        selectedEvmNetworkStore.selectedNetwork(family).flatMapLatest { network ->
            walletDao.observeByNetwork(network.name).map { rows -> rows.map { it.toDomain() } }
        }

    override fun observeWallet(id: String): Flow<EthereumWallet?> =
        walletDao.observeById(id).map { it?.toDomain() }

    override fun generateMnemonic(): List<String> = keyEngine.generateMnemonic()

    override suspend fun createWallet(
        network: EvmNetwork,
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
        network: EvmNetwork,
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
            network = EvmNetwork.valueOf(wallet.network),
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

    override suspend fun getCachedTransactions(walletId: String): EvmTransactionPage? {
        val cache = transactionDao.cacheForWallet(walletId) ?: return null
        val transactions = transactionDao.listByWalletId(walletId).map { it.toDomain() }
        return EvmTransactionPage(
            transactions = transactions,
            nextCursor = cache.nextCursor(json),
            hasMore = cache.hasMore,
        )
    }

    override suspend fun getTransactions(
        walletId: String,
        afterCursor: EvmTransactionPaginationCursor?,
    ): EvmTransactionPage {
        val wallet = walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        val page = transactionRemote.getAddressTransactions(
            network = EvmNetwork.valueOf(wallet.network),
            address = wallet.address,
            afterCursor = afterCursor,
        )
        persistTransactions(walletId, page, replace = afterCursor == null)
        return page
    }

    override fun isValidAddress(address: String): Boolean =
        keyEngine.isValidAddress(address)

    override suspend fun getFeeData(walletId: String): EvmFeeData {
        val wallet = walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        return remote.getFeeData(EvmNetwork.valueOf(wallet.network))
    }

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountWei: BigInteger,
        gasPreset: EvmGasPreset,
    ): String {
        if (amountWei <= BigInteger.ZERO) error("Enter an amount greater than zero")
        val wallet = walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        val mnemonic = mnemonicStore.loadMnemonic(walletId)
            ?: error("Wallet keys not found")
        val passphrase = mnemonicStore.loadPassphrase(walletId)
        val network = EvmNetwork.valueOf(wallet.network)
        val nonce = remote.getTransactionCount(network, wallet.address)
        val feeData = remote.getFeeData(network)
        val estimatedGas = runCatching {
            remote.estimateGas(
                network = network,
                from = wallet.address,
                to = recipientAddress,
                valueWei = amountWei,
            )
        }.getOrDefault(EvmGasQuotes.SIMPLE_TRANSFER_GAS_LIMIT)
        val gasLimit = maxOf(estimatedGas, EvmGasQuotes.SIMPLE_TRANSFER_GAS_LIMIT)
        val quote = EvmGasQuotes.quote(feeData, gasPreset, gasLimit)
        val balanceWei = wallet.balanceWei?.let { BigInteger(it) } ?: BigInteger.ZERO
        val totalNeeded = amountWei.add(BigInteger(quote.estimatedFeeWei))
        if (balanceWei < totalNeeded) error("Insufficient funds")
        val signedHex = withContext(Dispatchers.Default) {
            keyEngine.buildAndSignSend(
                mnemonicWords = mnemonic.split(" "),
                passphrase = passphrase,
                chainId = network.chainId,
                to = recipientAddress,
                valueWei = amountWei,
                nonce = nonce,
                gasLimit = quote.gasLimit,
                maxPriorityFeePerGasWei = BigInteger(quote.maxPriorityFeePerGasWei),
                maxFeePerGasWei = BigInteger(quote.maxFeePerGasWei),
            )
        }
        return remote.sendRawTransaction(network, signedHex)
    }

    private suspend fun persistTransactions(
        walletId: String,
        page: EvmTransactionPage,
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
