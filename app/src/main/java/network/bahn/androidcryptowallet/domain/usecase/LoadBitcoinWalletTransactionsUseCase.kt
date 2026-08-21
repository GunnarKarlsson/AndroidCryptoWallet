package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

class LoadBitcoinWalletTransactionsUseCase @Inject constructor(
    private val repository: BitcoinWalletRepository,
) {
    suspend operator fun invoke(
        walletId: String,
        afterTxid: String? = null,
    ) = repository.getTransactions(walletId, afterTxid)
}
