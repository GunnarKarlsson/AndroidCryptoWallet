package network.bahn.androidcryptowallet.ui.ethereum.setup

import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork

data class EthereumSetupUiState(
    val family: EvmFamily = EvmFamily.ETHEREUM,
    val availableNetworks: List<EvmNetwork> = EvmNetwork.networksFor(EvmFamily.ETHEREUM),
    val createNetwork: EvmNetwork = EvmNetwork.SEPOLIA,
    val mnemonicWords: List<String> = emptyList(),
    val passphrase: String = "",
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface EthereumSetupEvent {
    data object WalletCreated : EthereumSetupEvent
}
