package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.data.local.db.EvmWalletDao
import network.bahn.androidcryptowallet.data.local.db.toEntity
import network.bahn.androidcryptowallet.data.local.secure.EthereumMnemonicStore
import network.bahn.androidcryptowallet.domain.model.EvmWallet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-inserts HD Ethereum public rows by deriving the address from the encrypted
 * mnemonic if Room was dropped. Does not delete existing rows.
 */
@Singleton
class EthereumHdWalletRoomReconciler @Inject constructor(
    private val keyEngine: EthereumKeyEngine,
    private val mnemonicStore: EthereumMnemonicStore,
    private val walletDao: EvmWalletDao,
) {
    suspend fun reconcile() {
        mnemonicStore.listHdWalletIds().forEach { walletId ->
            val mnemonic = mnemonicStore.loadMnemonic(walletId) ?: return@forEach
            val network = mnemonicStore.loadNetwork(walletId) ?: return@forEach
            val passphrase = mnemonicStore.loadPassphrase(walletId)
            val derived = keyEngine.deriveReceiveAddress(
                mnemonicWords = mnemonic.split(" "),
                passphrase = passphrase,
            )
            walletDao.insertIgnore(
                EvmWallet(
                    id = walletId,
                    network = network,
                    address = derived.address,
                    derivationIndex = derived.index,
                ).toEntity(),
            )
        }
    }
}
