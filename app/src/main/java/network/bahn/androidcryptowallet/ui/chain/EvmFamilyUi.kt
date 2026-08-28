package network.bahn.androidcryptowallet.ui.chain

import network.bahn.androidcryptowallet.R
import network.bahn.androidcryptowallet.domain.model.EvmFamily

val EvmFamily.walletsTitleRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.string.ethereum_wallets_title
        EvmFamily.BSC -> R.string.bsc_wallets_title
    }

val EvmFamily.chainIconRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.drawable.ic_chain_ethereum
        EvmFamily.BSC -> R.drawable.ic_chain_bsc
    }

val EvmFamily.walletListItemLabelRes: Int
    get() = when (this) {
        EvmFamily.ETHEREUM -> R.string.ethereum_wallet_list_item_label
        EvmFamily.BSC -> R.string.bsc_wallet_list_item_label
    }

fun SupportedChain.toEvmFamily(): EvmFamily? = when (this) {
    SupportedChain.ETHEREUM -> EvmFamily.ETHEREUM
    SupportedChain.BSC -> EvmFamily.BSC
    SupportedChain.BITCOIN -> null
}
