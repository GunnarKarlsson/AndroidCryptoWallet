package network.bahn.androidcryptowallet

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.repository.WalletCatalogReadiness
import javax.inject.Inject

@HiltAndroidApp
class WalletApplication : Application() {
    @Inject
    lateinit var walletCatalogReadiness: WalletCatalogReadiness

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            walletCatalogReadiness.initialize()
        }
    }
}
