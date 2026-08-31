package network.bahn.androidcryptowallet.ui.home

import network.bahn.androidcryptowallet.domain.model.PortfolioHolding
import network.bahn.androidcryptowallet.domain.model.WalletNetworkMode

data class HomeUiState(
    val holdings: List<PortfolioHolding> = emptyList(),
    val assetCount: Int = 0,
    val totalFiatFormatted: String? = null,
    val networkMode: WalletNetworkMode = WalletNetworkMode.TESTNET,
    /** Spinner on the total card while balance refresh is in progress. */
    val isTotalLoading: Boolean = false,
    /** Spinner below the card while the chain list is not yet available. */
    val isHoldingsLoading: Boolean = true,
)
