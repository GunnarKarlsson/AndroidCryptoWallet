package network.bahn.androidcryptowallet.domain.usecase

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

class ObserveBitcoinWalletExistsUseCase @Inject constructor(
    private val repository: BitcoinWalletRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeWalletExists()
}
