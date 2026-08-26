package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.data.local.db.EthereumWalletDao
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.data.local.db.toEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedEthereumNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.data.remote.EthereumRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.EthereumKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
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
    private val selectedEthereumNetworkStore: SelectedEthereumNetworkStore,
    private val remote: EthereumRemoteDataSource,
    private val timeProvider: TimeProvider,
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
}
