package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import javax.inject.Inject

class SetBitcoinNetworkUseCase @Inject constructor(
    private val repository: BitcoinNetworkStatusRepository,
) {
    suspend operator fun invoke(network: BitcoinNetwork) = repository.setNetwork(network)
}
