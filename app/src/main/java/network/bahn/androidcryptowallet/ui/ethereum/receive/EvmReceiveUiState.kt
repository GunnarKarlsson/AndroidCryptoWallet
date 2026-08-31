package network.bahn.androidcryptowallet.ui.ethereum.receive

import network.bahn.androidcryptowallet.domain.model.EvmFamily

data class EvmReceiveUiState(
    val address: String? = null,
    val networkLabel: String? = null,
    val family: EvmFamily? = null,
    val paymentUri: String? = null,
)
