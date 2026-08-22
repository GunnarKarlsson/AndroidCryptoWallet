package network.bahn.androidcryptowallet.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object BitcoinWalletListRoute

@Serializable
data object BitcoinCreateGraphRoute

@Serializable
data object BitcoinSelectNetworkRoute

@Serializable
data object BitcoinCreateWalletRoute

@Serializable
data object BitcoinConfirmMnemonicRoute

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
