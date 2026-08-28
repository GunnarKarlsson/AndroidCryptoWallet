package network.bahn.androidcryptowallet.ui.ethereum.list

import network.bahn.androidcryptowallet.domain.model.EvmFamily
import network.bahn.androidcryptowallet.domain.model.EvmNetwork
import network.bahn.androidcryptowallet.domain.model.EthereumWallet

data class EthereumWalletListUiState(
    val family: EvmFamily = EvmFamily.ETHEREUM,
    val availableNetworks: List<EvmNetwork> = EvmNetwork.networksFor(EvmFamily.ETHEREUM),
    val selectedNetwork: EvmNetwork = EvmNetwork.SEPOLIA,
    val wallets: List<EthereumWallet> = emptyList(),
    val isLoading: Boolean = true,
)
