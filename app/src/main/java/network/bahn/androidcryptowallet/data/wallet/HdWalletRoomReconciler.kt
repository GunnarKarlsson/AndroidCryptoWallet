package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.toEntity
import network.bahn.androidcryptowallet.data.local.secure.BitcoinMnemonicStore
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet
import network.bahn.androidcryptowallet.domain.model.BitcoinWalletKind
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-inserts HD wallet public rows from the encrypted snapshot if Room was dropped.
 * Does not touch watch-only mock ids or delete existing HD rows.
 */
@Singleton
class HdWalletRoomReconciler @Inject constructor(
    private val mnemonicStore: BitcoinMnemonicStore,
    private val walletDao: BitcoinWalletDao,
) {
    suspend fun reconcile() {
        mnemonicStore.listHdWalletIds().forEach { walletId ->
            if (walletId.startsWith(MOCK_ID_PREFIX)) return@forEach
            val snapshot = mnemonicStore.loadPublic(walletId) ?: return@forEach
            walletDao.insertIgnore(
                BitcoinWallet(
                    id = snapshot.id,
                    network = snapshot.network,
                    receiveAddress = snapshot.receiveAddress,
                    derivationIndex = snapshot.derivationIndex,
                    scriptType = snapshot.scriptType,
                    kind = BitcoinWalletKind.HD,
                ).toEntity(),
            )
        }
    }

    private companion object {
        const val MOCK_ID_PREFIX = "mock:"
    }
}
