package network.bahn.androidcryptowallet.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.ConsolidatedTransaction

interface ConsolidatedTransactionRepository {
    fun observeTransactions(): Flow<List<ConsolidatedTransaction>>

    suspend fun refreshAllTransactions()
}
