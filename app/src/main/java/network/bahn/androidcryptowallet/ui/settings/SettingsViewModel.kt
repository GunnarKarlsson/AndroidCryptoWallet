package network.bahn.androidcryptowallet.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bahn.androidcryptowallet.domain.repository.ProviderSettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    providerSettingsRepository: ProviderSettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = providerSettingsRepository.observeProviders()
        .map { providers ->
            SettingsUiState(
                groups = providers
                    .groupBy { it.groupLabel }
                    .entries
                    .sortedBy { (label, _) -> label.lowercase() }
                    .map { (label, items) ->
                        ProviderGroup(label = label, providers = items)
                    },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )
}
