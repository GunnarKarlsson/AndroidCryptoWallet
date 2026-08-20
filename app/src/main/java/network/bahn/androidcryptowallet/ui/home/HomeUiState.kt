package network.bahn.androidcryptowallet.ui.home

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

data class HomeUiState(
    val selectedNetwork: BitcoinNetwork = BitcoinNetwork.TESTNET4,
    val blockHeight: Long? = null,
    val updatedAtMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
