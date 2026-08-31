package network.bahn.androidcryptowallet.ui.settings

import network.bahn.androidcryptowallet.domain.model.ProviderSetting

data class SettingsUiState(
    val groups: List<ProviderGroup> = emptyList(),
)

data class ProviderGroup(
    val label: String,
    val providers: List<ProviderSetting>,
)
