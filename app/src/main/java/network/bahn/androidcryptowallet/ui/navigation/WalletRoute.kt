package network.bahn.androidcryptowallet.ui.navigation

import network.bahn.androidcryptowallet.domain.model.EvmFamily
import kotlinx.serialization.Serializable

@Serializable
data object ChainSelectRoute

@Serializable
data object BitcoinWalletListRoute

@Serializable
data class EvmWalletListRoute(val family: EvmFamily)

@Serializable
data object BitcoinCreateGraphRoute

@Serializable
data object BitcoinSelectNetworkRoute

@Serializable
data object BitcoinCreateWalletRoute

@Serializable
data object BitcoinConfirmMnemonicRoute

@Serializable
data object BitcoinRestoreGraphRoute

@Serializable
data object BitcoinRestoreSelectNetworkRoute

@Serializable
data object BitcoinRestoreWalletRoute

@Serializable
data object BitcoinNetworkStatusRoute

@Serializable
data class BitcoinWalletDetailsRoute(val walletId: String)

@Serializable
data class BitcoinSendRoute(val walletId: String)

@Serializable
data class BitcoinReceiveRoute(val walletId: String)

@Serializable
data class BitcoinEditWalletRoute(val walletId: String)

@Serializable
data class EvmCreateGraphRoute(val family: EvmFamily)

@Serializable
data object EthereumSelectNetworkRoute

@Serializable
data object EthereumCreateWalletRoute

@Serializable
data object EthereumConfirmMnemonicRoute

@Serializable
data class EvmRestoreGraphRoute(val family: EvmFamily)

@Serializable
data object EthereumRestoreSelectNetworkRoute

@Serializable
data object EthereumRestoreWalletRoute

@Serializable
data class EthereumWalletDetailsRoute(val walletId: String)

@Serializable
data class EthereumSendRoute(val walletId: String)

@Serializable
data class EthereumReceiveRoute(val walletId: String)

@Serializable
data class EthereumEditWalletRoute(val walletId: String)
