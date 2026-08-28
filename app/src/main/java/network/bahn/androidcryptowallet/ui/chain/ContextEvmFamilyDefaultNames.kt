package network.bahn.androidcryptowallet.ui.chain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextEvmFamilyDefaultNames @Inject constructor(
    @ApplicationContext private val context: Context,
) : EvmFamilyDefaultNames {
    override fun walletListName(family: network.bahn.androidcryptowallet.domain.model.EvmFamily): String =
        context.getString(family.walletListItemLabelRes)
}
