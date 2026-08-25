package network.bahn.androidcryptowallet.ui.ethereum.setup

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork

data class EthereumSetupUiState(
    val createNetwork: EthereumNetwork = EthereumNetwork.SEPOLIA,
    val mnemonicWords: List<String> = emptyList(),
    val passphrase: String = "",
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface EthereumSetupEvent {
    data object WalletCreated : EthereumSetupEvent
}
