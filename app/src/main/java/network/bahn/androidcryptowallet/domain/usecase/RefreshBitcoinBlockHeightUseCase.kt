package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.repository.BitcoinNetworkStatusRepository
import javax.inject.Inject

class RefreshBitcoinBlockHeightUseCase @Inject constructor(
    private val repository: BitcoinNetworkStatusRepository,
) {
    suspend operator fun invoke() = repository.refreshBlockHeight()
}
