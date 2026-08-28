package network.bahn.androidcryptowallet.ui.chain

import network.bahn.androidcryptowallet.domain.model.EvmFamily

fun interface EvmFamilyDefaultNames {
    fun walletListName(family: EvmFamily): String
}
