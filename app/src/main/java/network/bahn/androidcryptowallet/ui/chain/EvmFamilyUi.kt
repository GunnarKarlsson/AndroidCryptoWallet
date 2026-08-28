package network.bahn.androidcryptowallet.ui.chain

import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EvmFamily

val EvmFamily.walletsTitleRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.string.ethereum_wallets_title
        EvmFamily.BSC -> R.string.bsc_wallets_title
        EvmFamily.POLYGON -> R.string.polygon_wallets_title
        EvmFamily.ARBITRUM -> R.string.arbitrum_wallets_title
        EvmFamily.BASE -> R.string.base_wallets_title
        EvmFamily.OPTIMISM -> R.string.optimism_wallets_title
        EvmFamily.AVALANCHE -> R.string.avalanche_wallets_title
    }

val EvmFamily.chainIconRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.drawable.ic_chain_ethereum
        EvmFamily.BSC -> R.drawable.ic_chain_bsc
        EvmFamily.POLYGON -> R.drawable.ic_chain_polygon
        EvmFamily.ARBITRUM -> R.drawable.ic_chain_arbitrum
        EvmFamily.BASE -> R.drawable.ic_chain_base
        EvmFamily.OPTIMISM -> R.drawable.ic_chain_optimism
        EvmFamily.AVALANCHE -> R.drawable.ic_chain_avalanche
    }

val EvmFamily.walletListItemLabelRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.string.ethereum_wallet_list_item_label
        EvmFamily.BSC -> R.string.bsc_wallet_list_item_label
        EvmFamily.POLYGON -> R.string.polygon_wallet_list_item_label
        EvmFamily.ARBITRUM -> R.string.arbitrum_wallet_list_item_label
        EvmFamily.BASE -> R.string.base_wallet_list_item_label
        EvmFamily.OPTIMISM -> R.string.optimism_wallet_list_item_label
        EvmFamily.AVALANCHE -> R.string.avalanche_wallet_list_item_label
    }

val EvmFamily.receiveClipboardLabelRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.string.receive_clipboard_label_eth
        EvmFamily.BSC -> R.string.receive_clipboard_label_bsc
        EvmFamily.POLYGON -> R.string.receive_clipboard_label_polygon
        EvmFamily.ARBITRUM -> R.string.receive_clipboard_label_arbitrum
        EvmFamily.BASE -> R.string.receive_clipboard_label_base
        EvmFamily.OPTIMISM -> R.string.receive_clipboard_label_optimism
        EvmFamily.AVALANCHE -> R.string.receive_clipboard_label_avalanche
    }

fun SupportedChain.toEvmFamily(): EvmFamily? = when (this) {
    SupportedChain.ETHEREUM -> EvmFamily.ETHEREUM
    SupportedChain.BSC -> EvmFamily.BSC
    SupportedChain.POLYGON -> EvmFamily.POLYGON
    SupportedChain.ARBITRUM -> EvmFamily.ARBITRUM
    SupportedChain.BASE -> EvmFamily.BASE
    SupportedChain.OPTIMISM -> EvmFamily.OPTIMISM
    SupportedChain.AVALANCHE -> EvmFamily.AVALANCHE
    SupportedChain.BITCOIN -> null
}
