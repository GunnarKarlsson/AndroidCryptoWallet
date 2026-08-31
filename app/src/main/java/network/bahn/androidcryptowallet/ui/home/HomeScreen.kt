package network.bahn.androidcryptowallet.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.bahn.androidcryptowallet.domain.model.PortfolioHoldingDestination

@Composable
fun HomeScreen(
    onAddWallet: () -> Unit,
    onHoldingClick: (PortfolioHoldingDestination) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEnter()
    }

    HomeContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onAddWallet = onAddWallet,
        onHoldingClick = onHoldingClick,
    )
}
