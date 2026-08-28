package network.bahn.androidcryptowallet.data.wallet

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletCatalogInitializer @Inject constructor(
    private val hdWalletRoomReconciler: HdWalletRoomReconciler,
    private val watchOnlyBitcoinWalletSeeder: WatchOnlyBitcoinWalletSeeder,
    private val evmHdWalletRoomReconciler: EvmHdWalletRoomReconciler,
) : WalletCatalogReadiness {
    private val ready = MutableStateFlow(false)

    override fun observeReady(): Flow<Boolean> = ready.asStateFlow()

    override suspend fun initialize() {
        try {
            try {
                hdWalletRoomReconciler.reconcile()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reconcile HD wallets", e)
            }
            try {
                watchOnlyBitcoinWalletSeeder.seed()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed watch-only wallets", e)
            }
            try {
                evmHdWalletRoomReconciler.reconcile()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reconcile Ethereum HD wallets", e)
            }
        } finally {
            ready.value = true
        }
    }

    private companion object {
        const val TAG = "WalletCatalog"
    }
}
