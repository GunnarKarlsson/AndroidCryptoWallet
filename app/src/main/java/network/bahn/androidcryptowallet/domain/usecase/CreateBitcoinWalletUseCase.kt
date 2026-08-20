package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

class CreateBitcoinWalletUseCase @Inject constructor(
    private val repository: BitcoinWalletRepository,
) {
    suspend operator fun invoke(mnemonicWords: List<String>, passphrase: String?) =
        repository.createWallet(mnemonicWords, passphrase)
}
