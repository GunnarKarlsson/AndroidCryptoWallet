package network.bahn.androidcryptowallet.domain.usecase

import network.bahn.androidcryptowallet.domain.repository.BitcoinWalletRepository
import javax.inject.Inject

class SendBitcoinUseCase @Inject constructor(
    private val repository: BitcoinWalletRepository,
) {
    suspend operator fun invoke(
        walletId: String,
        recipientAddress: String,
        amountSatoshis: Long,
        feeRateSatPerVbyte: Long,
    ): String = repository.send(
        walletId = walletId,
        recipientAddress = recipientAddress,
        amountSatoshis = amountSatoshis,
        feeRateSatPerVbyte = feeRateSatPerVbyte,
    )
}
