package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.data.local.db.toEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.remote.BitcoinRemoteDataSource
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.domain.TimeProvider
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
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
        )
        walletDao.insert(wallet.toEntity())
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
}
