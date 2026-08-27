package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import network.bahn.androidcryptowallet.data.local.db.BitcoinTransactionDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletTxCacheEntity
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.data.local.db.toEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinTransactionPage
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BitcoinWalletRepositoryImpl @Inject constructor(
    private val keyEngine: BitcoinKeyEngine,
    private val mnemonicStore: BitcoinMnemonicStore,
    private val walletDao: BitcoinWalletDao,
    private val transactionDao: BitcoinTransactionDao,
    private val selectedBitcoinNetworkStore: SelectedBitcoinNetworkStore,
    private val remote: BitcoinRemoteDataSource,
    private val timeProvider: TimeProvider,
) : BitcoinWalletRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeWallets(): Flow<List<BitcoinWallet>> =
        selectedBitcoinNetworkStore.selectedNetwork.flatMapLatest { network ->
            walletDao.observeByNetwork(network.name).map { rows -> rows.map { it.toDomain() } }
        }

    override fun observeWallet(id: String): Flow<BitcoinWallet?> =
        walletDao.observeById(id).map { it?.toDomain() }

    override fun generateMnemonic(): List<String> = keyEngine.generateMnemonic()

    override suspend fun createWallet(
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) {
        // BIP-39 checksum/wordlist, then BIP-32/BIP-84 derive for [network] only.
        keyEngine.validateMnemonic(mnemonicWords)
        val derived = keyEngine.deriveReceiveAddress(mnemonicWords, passphrase, network)
        val wallet = BitcoinWallet(
            id = UUID.randomUUID().toString(),
            network = network,
            receiveAddress = derived.address,
            derivationIndex = derived.index,
            scriptType = derived.scriptType,
            kind = BitcoinWalletKind.HD,
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
        network: BitcoinNetwork,
        mnemonicWords: List<String>,
        passphrase: String?,
    ) {
        keyEngine.validateMnemonic(mnemonicWords)
        val derived = keyEngine.deriveReceiveAddress(mnemonicWords, passphrase, network)
        val existing = walletDao.findByNetworkAndAddress(network.name, derived.address)
        if (existing != null) return
        createWallet(network, mnemonicWords, passphrase)
    }

    override suspend fun refreshBalance(walletId: String) {
        val wallet = walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        val balance = remote.getAddressBalance(
            network = BitcoinNetwork.valueOf(wallet.network),
            address = wallet.receiveAddress,
        )
        walletDao.updateBalance(
            id = walletId,
            confirmedSatoshis = balance.confirmedSatoshis,
            unconfirmedSatoshis = balance.unconfirmedSatoshis,
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

    override suspend fun getCachedTransactions(walletId: String): BitcoinTransactionPage? {
        val cache = transactionDao.cacheForWallet(walletId) ?: return null
        val transactions = transactionDao.listByWalletId(walletId).map { it.toDomain() }
        return BitcoinTransactionPage(
            transactions = transactions,
            lastConfirmedTxid = cache.lastConfirmedTxid,
            hasMore = cache.hasMore,
        )
    }

    override suspend fun getTransactions(
        walletId: String,
        afterTxid: String?,
    ): BitcoinTransactionPage {
        val wallet = walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        val page = remote.getAddressTransactions(
            network = BitcoinNetwork.valueOf(wallet.network),
            address = wallet.receiveAddress,
            afterTxid = afterTxid,
        )
        persistTransactions(walletId, page, replace = afterTxid == null)
        return page
    }

    override fun isValidAddress(
        network: BitcoinNetwork,
        address: String,
    ): Boolean = keyEngine.isValidAddress(network, address)

    override suspend fun send(
        walletId: String,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
    ): String {
        val wallet = walletDao.observeById(walletId).first()
            ?: error("Wallet not found")
        if (wallet.kind == BitcoinWalletKind.WATCH_ONLY.name) {
            error("Watch-only wallets cannot send")
        }
        if (amountSatoshis <= 0L) error("Enter an amount greater than zero")
        val mnemonic = mnemonicStore.loadMnemonic(walletId)
            ?: error("Wallet keys not found")
        val passphrase = mnemonicStore.loadPassphrase(walletId)
        val network = BitcoinNetwork.valueOf(wallet.network)
        val confirmedUtxos = remote.getAddressUtxos(network, wallet.receiveAddress)
            .filter { it.confirmed }
        if (confirmedUtxos.isEmpty()) error("Insufficient funds")
        val fundingTxHexes = confirmedUtxos.map { it.txid }.distinct().map { txid ->
            remote.getTransactionHex(network, txid)
        }
        val signed = withContext(Dispatchers.Default) {
            keyEngine.buildAndSignSend(
                mnemonicWords = mnemonic.split(" "),
                passphrase = passphrase,
                network = network,
                fundingTxHexes = fundingTxHexes,
                recipientAddress = recipientAddress,
                amountSatoshis = amountSatoshis,
                feeRateSatPerVbyte = feeRateSatPerVbyte,
                changeAddress = wallet.receiveAddress,
            )
        }
        return remote.broadcastTransaction(network, signed.rawHex)
    }

    private suspend fun persistTransactions(
        walletId: String,
        page: BitcoinTransactionPage,
        replace: Boolean,
    ) {
        val startIndex = if (replace) 0 else transactionDao.maxSortIndex(walletId) + 1
        val entities = page.transactions.mapIndexed { index, tx ->
            tx.toEntity(walletId = walletId, sortIndex = startIndex + index)
        }
        val cache = BitcoinWalletTxCacheEntity(
            walletId = walletId,
            lastConfirmedTxid = page.lastConfirmedTxid,
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
