package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

class ValidateBitcoinAddressUseCase @Inject constructor(
    private val repository: BitcoinWalletRepository,
) {
    operator fun invoke(
        network: BitcoinNetwork,
        address: String,
    ): Boolean = repository.isValidAddress(network, address)
}
