package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.PortfolioHolding

interface PortfolioRepository {
    /** Non-zero native holdings on each family's currently selected network, sorted alphabetically. */
    fun observeHoldings(): Flow<List<PortfolioHolding>>

    suspend fun refreshAllBalances()
}
