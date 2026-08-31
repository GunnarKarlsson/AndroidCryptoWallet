package network.bahn.androidcryptowallet.ui.settings

data class ProviderEditUiState(
    val providerId: String = "",
    val title: String = "",
    val url: String = "",
    val defaultUrl: String = "",
    val isOverridden: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)
