package network.bahn.androidcryptowallet.ui.bitcoin.details

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet

data class BitcoinWalletDetailsUiState(
    val wallet: BitcoinWallet? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) {
    val network: BitcoinNetwork? get() = wallet?.network
    val receiveAddress: String? get() = wallet?.receiveAddress
    val confirmedBalanceSatoshis: Long? get() = wallet?.confirmedBalanceSatoshis
    val unconfirmedBalanceSatoshis: Long? get() = wallet?.unconfirmedBalanceSatoshis
    val balanceUpdatedAtMillis: Long? get() = wallet?.balanceUpdatedAtMillis
}
