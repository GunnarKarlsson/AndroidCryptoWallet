package network.bahn.androidcryptowallet.data.wallet

import network.bahn.androidcryptowallet.data.local.db.BitcoinWalletDao
import network.bahn.androidcryptowallet.data.local.db.toEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Upserts watch-only mock wallets from [MockBitcoinWalletConfig] and prunes stale `mock:` rows.
 * Does not touch the mnemonic store.
 */
@Singleton
class WatchOnlyBitcoinWalletSeeder @Inject constructor(
    private val config: MockBitcoinWalletConfig,
    private val walletDao: BitcoinWalletDao,
) {
    suspend fun seed() {
        val desired = config.desiredWallets()
        val desiredIds = desired.map { it.id }.toSet()
        val staleIds = walletDao.mockWalletIds().filter { it !in desiredIds }
        if (staleIds.isNotEmpty()) {
            walletDao.deleteByIds(staleIds)
        }
        desired.forEach { wallet ->
            walletDao.insertIgnore(wallet.toEntity())
        }
    }
}
