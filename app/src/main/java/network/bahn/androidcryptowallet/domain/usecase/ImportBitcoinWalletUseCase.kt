package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

class ImportBitcoinWalletUseCase @Inject constructor(
    private val repository: BitcoinWalletRepository,
) {
    suspend operator fun invoke(mnemonic: String, passphrase: String?) =
        repository.importWallet(mnemonic, passphrase)
}
