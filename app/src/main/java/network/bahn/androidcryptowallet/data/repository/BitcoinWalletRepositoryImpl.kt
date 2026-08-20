package network.bahn.androidcryptowallet.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import network.bahn.androidcryptowallet.data.local.db.BitcoinReceiveAddressDao
import network.bahn.androidcryptowallet.data.local.db.toDomain
import network.bahn.androidcryptowallet.data.local.db.toEntity
import network.bahn.androidcryptowallet.data.local.prefs.SelectedBitcoinNetworkStore
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.data.wallet.BitcoinKeyEngine
import network.bahn.androidcryptowallet.domain.model.BitcoinReceiveAddress
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BitcoinWalletRepositoryImpl @Inject constructor(
    private val keyEngine: BitcoinKeyEngine,
    private val mnemonicStore: BitcoinMnemonicStore,
    private val receiveAddressDao: BitcoinReceiveAddressDao,
    private val selectedBitcoinNetworkStore: SelectedBitcoinNetworkStore,
) : BitcoinWalletRepository {
    override fun observeWalletExists(): Flow<Boolean> =
        receiveAddressDao.observeCount().map { count -> count > 0 }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeReceiveAddress(): Flow<BitcoinReceiveAddress?> =
        selectedBitcoinNetworkStore.selectedNetwork.flatMapLatest { network ->
            receiveAddressDao.observe(network.name).map { entity -> entity?.toDomain() }
        }

    override fun generateMnemonic(): List<String> = keyEngine.generateMnemonic()

    override suspend fun createWallet(mnemonicWords: List<String>, passphrase: String?) {
        persistWallet(mnemonicWords, passphrase)
    }

    override suspend fun importWallet(mnemonic: String, passphrase: String?) {
        val words = mnemonic.trim().lowercase().split(WHITESPACE).filter { it.isNotEmpty() }
        persistWallet(words, passphrase)
    }

    private suspend fun persistWallet(mnemonicWords: List<String>, passphrase: String?) {
        check(!mnemonicStore.hasWallet()) { "A Bitcoin wallet already exists on this device" }
        // BIP-39 checksum/wordlist, then BIP-32/BIP-84 derive; persist seed, cache public addresses.
        keyEngine.validateMnemonic(mnemonicWords)
        val addresses = keyEngine.deriveReceiveAddresses(mnemonicWords, passphrase)
        mnemonicStore.save(
            mnemonic = mnemonicWords.joinToString(" "),
            passphrase = passphrase?.takeIf { it.isNotEmpty() },
        )
        receiveAddressDao.upsertAll(addresses.map { it.toEntity() })
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
