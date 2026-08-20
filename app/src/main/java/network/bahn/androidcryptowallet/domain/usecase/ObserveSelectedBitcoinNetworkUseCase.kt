package network.bahn.androidcryptowallet.domain.usecase

import kotlinx.coroutines.flow.Flow
import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import javax.inject.Inject

class ObserveSelectedBitcoinNetworkUseCase @Inject constructor(
    private val repository: BitcoinNetworkStatusRepository,
) {
    operator fun invoke(): Flow<BitcoinNetwork> = repository.selectedNetwork()
}
