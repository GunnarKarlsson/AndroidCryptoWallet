package network.bahn.androidcryptowallet.ui.bitcoin.setup

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

data class BitcoinSetupUiState(
    val createNetwork: BitcoinNetwork = BitcoinNetwork.TESTNET4,
    val mnemonicWords: List<String> = emptyList(),
    val passphrase: String = "",
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface BitcoinSetupEvent {
    data object WalletCreated : BitcoinSetupEvent
}
