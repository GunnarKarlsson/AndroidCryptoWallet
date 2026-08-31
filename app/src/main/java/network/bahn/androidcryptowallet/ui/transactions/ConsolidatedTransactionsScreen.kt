package network.bahn.androidcryptowallet.ui.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.bahn.androidcryptowallet.domain.model.ConsolidatedTransaction

@Composable
fun ConsolidatedTransactionsScreen(
    onTransactionClick: (ConsolidatedTransaction) -> Unit,
    viewModelStoreOwner: ViewModelStoreOwner,
    viewModel: ConsolidatedTransactionsViewModel = hiltViewModel(viewModelStoreOwner),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ConsolidatedTransactionsContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onTransactionClick = onTransactionClick,
        onNetworkModeSelected = viewModel::setNetworkMode,
    )
}
