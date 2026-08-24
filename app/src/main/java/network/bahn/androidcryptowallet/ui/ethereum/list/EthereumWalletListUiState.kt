package network.bahn.androidcryptowallet.ui.ethereum.list

import network.bahn.androidcryptowallet.domain.model.EthereumNetwork

data class EthereumWalletListUiState(
    val selectedNetwork: EthereumNetwork = EthereumNetwork.SEPOLIA,
)
