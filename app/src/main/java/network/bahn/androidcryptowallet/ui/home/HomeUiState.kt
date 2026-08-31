package network.bahn.androidcryptowallet.ui.home

import network.bahn.androidcryptowallet.domain.model.PortfolioHolding

data class HomeUiState(
    val holdings: List<PortfolioHolding> = emptyList(),
    val assetCount: Int = 0,
    val totalFiatFormatted: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)
