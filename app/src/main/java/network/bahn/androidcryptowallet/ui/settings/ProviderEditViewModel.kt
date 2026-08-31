package network.bahn.androidcryptowallet.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bahn.androidcryptowallet.domain.repository.ProviderSettingsRepository
import network.bahn.androidcryptowallet.ui.navigation.ProviderEditRoute
import javax.inject.Inject

@HiltViewModel
class ProviderEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val providerSettingsRepository: ProviderSettingsRepository,
) : ViewModel() {
    private val providerId: String = savedStateHandle.toRoute<ProviderEditRoute>().providerId
    private val editedUrl = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProviderEditUiState> = combine(
        providerSettingsRepository.observeProvider(providerId),
        editedUrl,
        isSaving,
        errorMessage,
    ) { provider, draftUrl, saving, error ->
        if (provider == null) {
            ProviderEditUiState(providerId = providerId, errorMessage = error)
        } else {
            ProviderEditUiState(
                providerId = provider.id,
                title = provider.label,
                url = draftUrl ?: provider.currentUrl,
                defaultUrl = provider.defaultUrl,
                isOverridden = provider.isOverridden,
                isSaving = saving,
                errorMessage = error,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProviderEditUiState(providerId = providerId),
    )

    fun onUrlChange(url: String) {
        editedUrl.value = url
        errorMessage.value = null
    }

    fun save() {
        val url = uiState.value.url
        if (url.isBlank()) {
            errorMessage.value = "URL cannot be blank"
            return
        }
        viewModelScope.launch {
            isSaving.update { true }
            errorMessage.update { null }
            try {
                providerSettingsRepository.setUrl(providerId, url)
                editedUrl.update { null }
            } catch (e: Exception) {
                errorMessage.update { e.message ?: "Could not save URL" }
            } finally {
                isSaving.update { false }
            }
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            isSaving.update { true }
            errorMessage.update { null }
            try {
                providerSettingsRepository.resetToDefault(providerId)
                editedUrl.update { null }
            } catch (e: Exception) {
                errorMessage.update { e.message ?: "Could not reset URL" }
            } finally {
                isSaving.update { false }
            }
        }
    }
}
