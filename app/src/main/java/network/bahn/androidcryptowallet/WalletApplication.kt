package network.bahn.androidcryptowallet

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.data.wallet.HdWalletRoomReconciler
import network.bahn.androidcryptowallet.data.wallet.WatchOnlyBitcoinWalletSeeder
import javax.inject.Inject

@HiltAndroidApp
class WalletApplication : Application() {
    @Inject
    lateinit var hdWalletRoomReconciler: HdWalletRoomReconciler

    @Inject
    lateinit var watchOnlyBitcoinWalletSeeder: WatchOnlyBitcoinWalletSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
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
        }
    }

    private companion object {
        const val TAG = "WalletApplication"
    }
}
