package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

class GenerateBitcoinMnemonicUseCase @Inject constructor(
    private val repository: BitcoinWalletRepository,
) {
    operator fun invoke(): List<String> = repository.generateMnemonic()
}
