package network.bahn.androidcryptowallet.ui.bitcoin.setup

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork

data class BitcoinRestoreUiState(
    val restoreNetwork: BitcoinNetwork = BitcoinNetwork.TESTNET4,
    val mnemonicWords: List<String> = List(RESTORE_MNEMONIC_WORD_COUNT) { "" },
    val passphrase: String = "",
    val isRestoring: Boolean = false,
    val errorMessage: String? = null,
) {
    val canRestore: Boolean
        get() = mnemonicWords.size == RESTORE_MNEMONIC_WORD_COUNT &&
            mnemonicWords.all { it.isNotBlank() } &&
            !isRestoring
}

const val RESTORE_MNEMONIC_WORD_COUNT = 12

sealed interface BitcoinRestoreEvent {
    data object WalletRestored : BitcoinRestoreEvent
}
