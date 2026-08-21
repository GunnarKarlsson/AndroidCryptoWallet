package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

class GetCachedBitcoinWalletTransactionsUseCase @Inject constructor(
    private val repository: BitcoinWalletRepository,
) {
    suspend operator fun invoke(walletId: String) = repository.getCachedTransactions(walletId)
}
