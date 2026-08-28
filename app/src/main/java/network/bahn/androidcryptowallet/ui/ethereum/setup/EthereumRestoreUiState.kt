package network.bahn.androidcryptowallet.ui.ethereum.setup

import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork

data class EthereumRestoreUiState(
    val family: EvmFamily = EvmFamily.ETHEREUM,
    val availableNetworks: List<EvmNetwork> = EvmNetwork.networksFor(EvmFamily.ETHEREUM),
    val restoreNetwork: EvmNetwork = EvmNetwork.SEPOLIA,
    val mnemonicWords: List<String> = List(ETH_RESTORE_MNEMONIC_WORD_COUNT) { "" },
    val passphrase: String = "",
    val isRestoring: Boolean = false,
    val errorMessage: String? = null,
) {
    val canRestore: Boolean
        get() = mnemonicWords.size == ETH_RESTORE_MNEMONIC_WORD_COUNT &&
            mnemonicWords.all { it.isNotBlank() } &&
            !isRestoring
}

const val ETH_RESTORE_MNEMONIC_WORD_COUNT = 12

sealed interface EthereumRestoreEvent {
    data object WalletRestored : EthereumRestoreEvent
}
