package network.bahn.androidcryptowallet.domain.usecase

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.BitcoinNetworkStatus
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import javax.inject.Inject

class ObserveBitcoinNetworkStatusUseCase @Inject constructor(
    private val repository: BitcoinNetworkStatusRepository,
) {
    operator fun invoke(): Flow<BitcoinNetworkStatus?> = repository.observeStatus()
}
