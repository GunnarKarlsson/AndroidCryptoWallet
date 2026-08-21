package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Becomes ready after HD Room reconcile and watch-only seed have finished
 * (or failed). The wallet list stays in a loading state until then so an
 * empty catalog is not shown while existing wallets are still being restored.
 */
interface WalletCatalogReadiness {
    fun observeReady(): Flow<Boolean>
    suspend fun initialize()
}
