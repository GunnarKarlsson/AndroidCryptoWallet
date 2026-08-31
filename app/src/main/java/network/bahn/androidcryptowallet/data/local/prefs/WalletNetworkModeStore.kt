package network.bahn.androidcryptowallet.data.local.prefs

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode

interface WalletNetworkModeStore {
    fun observeMode(): Flow<WalletNetworkMode>
    suspend fun setMode(mode: WalletNetworkMode)
}
