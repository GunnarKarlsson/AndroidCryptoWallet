package network.bahn.androidcryptowallet.ui.bitcoin.receive

import network.bahn.androidcryptowallet.domain.model.BitcoinPaymentUri

data class BitcoinReceiveUiState(
    val address: String? = null,
    val networkLabel: String? = null,
) {
    val paymentUri: String?
        get() = address?.takeIf { it.isNotBlank() }?.let(BitcoinPaymentUri::fromAddress)
}
