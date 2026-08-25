package network.bahn.androidcryptowallet.ui.ethereum.list

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet

data class EthereumWalletListUiState(
    val selectedNetwork: EthereumNetwork = EthereumNetwork.SEPOLIA,
    val wallets: List<EthereumWallet> = emptyList(),
    val isLoading: Boolean = true,
)
