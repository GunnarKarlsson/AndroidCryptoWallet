package network.bahn.androidcryptowallet.ui.bitcoin.list

import network.bahn.androidcryptowallet.domain.model.BitcoinNetwork
import network.bahn.androidcryptowallet.domain.model.BitcoinWallet

data class BitcoinWalletListUiState(
    val selectedNetwork: BitcoinNetwork = BitcoinNetwork.TESTNET4,
    val wallets: List<BitcoinWallet> = emptyList(),
)
