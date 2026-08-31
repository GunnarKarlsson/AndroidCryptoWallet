package network.bahn.androidcryptowallet.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import network.bahn.androidcryptowallet.domain.model.ProviderSetting

@Composable
fun SettingsScreen(
    onProviderClick: (ProviderSetting) -> Unit,
    viewModelStoreOwner: ViewModelStoreOwner,
    viewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onProviderClick = onProviderClick,
    )
}
