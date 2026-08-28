package network.bahn.androidcryptowallet.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import network.bahn.androidcryptowallet.domain.model.EvmFamily

internal fun savedStateHandleForEvmWalletList(family: EvmFamily = EvmFamily.ETHEREUM): SavedStateHandle =
    SavedStateHandle(EvmWalletListRoute(family))

internal fun savedStateHandleForEvmCreateGraph(family: EvmFamily = EvmFamily.ETHEREUM): SavedStateHandle =
    SavedStateHandle(EvmCreateGraphRoute(family))

internal fun savedStateHandleForEvmRestoreGraph(family: EvmFamily = EvmFamily.ETHEREUM): SavedStateHandle =
    SavedStateHandle(EvmRestoreGraphRoute(family))
