package network.bahn.androidcryptowallet.ui.bitcoin

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

data class BitcoinHomeUiState(
    val selectedNetwork: BitcoinNetwork = BitcoinNetwork.TESTNET4,
    val blockHeight: Long? = null,
    val updatedAtMillis: Long? = null,
    val receiveAddress: String? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
