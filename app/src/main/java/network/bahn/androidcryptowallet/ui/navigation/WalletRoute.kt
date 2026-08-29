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
data object EvmSelectNetworkRoute

@Serializable
data object EvmCreateWalletRoute

@Serializable
data object EvmConfirmMnemonicRoute

@Serializable
data class EvmRestoreGraphRoute(val family: EvmFamily)

@Serializable
data object EvmRestoreSelectNetworkRoute

@Serializable
data object EvmRestoreWalletRoute

@Serializable
data class EvmWalletDetailsRoute(val walletId: String)

@Serializable
data class EvmSendRoute(val walletId: String)

@Serializable
data class EvmReceiveRoute(val walletId: String)

@Serializable
data class EvmEditWalletRoute(val walletId: String)
