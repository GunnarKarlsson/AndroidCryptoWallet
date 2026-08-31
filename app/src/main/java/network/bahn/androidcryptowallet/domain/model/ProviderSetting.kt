package network.bahn.androidcryptowallet.domain.model

data class ProviderSetting(
    val id: String,
    val groupLabel: String,
    val label: String,
    val currentUrl: String,
    val defaultUrl: String,
    val isOverridden: Boolean,
)
