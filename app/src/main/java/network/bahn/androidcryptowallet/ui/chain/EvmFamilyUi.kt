package network.bahn.androidcryptowallet.ui.chain

import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EvmFamily

val EvmFamily.walletsTitleRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.string.ethereum_wallets_title
        EvmFamily.BSC -> R.string.bsc_wallets_title
        EvmFamily.POLYGON -> R.string.polygon_wallets_title
    }

val EvmFamily.chainIconRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.drawable.ic_chain_ethereum
        EvmFamily.BSC -> R.drawable.ic_chain_bsc
        EvmFamily.POLYGON -> R.drawable.ic_chain_polygon
    }

val EvmFamily.walletListItemLabelRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.string.ethereum_wallet_list_item_label
        EvmFamily.BSC -> R.string.bsc_wallet_list_item_label
        EvmFamily.POLYGON -> R.string.polygon_wallet_list_item_label
    }

val EvmFamily.receiveClipboardLabelRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.string.receive_clipboard_label_eth
        EvmFamily.BSC -> R.string.receive_clipboard_label_bsc
        EvmFamily.POLYGON -> R.string.receive_clipboard_label_polygon
    }

fun SupportedChain.toEvmFamily(): EvmFamily? = when (this) {
    SupportedChain.ETHEREUM -> EvmFamily.ETHEREUM
    SupportedChain.BSC -> EvmFamily.BSC
    SupportedChain.POLYGON -> EvmFamily.POLYGON
    SupportedChain.BITCOIN -> null
}
