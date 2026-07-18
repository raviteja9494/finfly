/* Presentation-layer ViewModel managing the Firefly connection settings form. */
package com.teja.finfly.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.usecase.SaveSettingsUseCase
import com.teja.finfly.domain.usecase.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Manages editable credentials, masking, persistence progress, and connection-test feedback. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val saveSettings: SaveSettingsUseCase,
    private val testConnection: TestConnectionUseCase,
) : ViewModel() {
    private val form = MutableStateFlow(SettingsForm())

    val uiState = form.map { SettingsUiState.Success(it) as SettingsUiState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { saved ->
                form.value = form.value.copy(
                    serverUrl = saved.serverUrl,
                    bearerToken = saved.bearerToken,
                    lastSyncTime = saved.lastSyncTime,
                )
            }
        }
    }

    fun updateServerUrl(value: String) { form.value = form.value.copy(serverUrl = value, feedback = null) }
    fun updateBearerToken(value: String) { form.value = form.value.copy(bearerToken = value, feedback = null) }
    fun toggleTokenVisibility() { form.value = form.value.copy(showToken = !form.value.showToken) }

    fun test() {
        if (form.value.isTesting) return
        viewModelScope.launch {
            form.value = form.value.copy(isTesting = true, feedback = null)
            val result = testConnection(form.value.serverUrl, form.value.bearerToken)
            form.value = form.value.copy(
                isTesting = false,
                feedback = result.toFeedback(success = SettingsFeedback.CONNECTION_SUCCESS),
            )
        }
    }

    fun save() {
        if (form.value.isSaving) return
        viewModelScope.launch {
            form.value = form.value.copy(isSaving = true, feedback = null)
            val result = saveSettings(form.value.serverUrl, form.value.bearerToken)
            form.value = form.value.copy(
                isSaving = false,
                feedback = result.toFeedback(success = SettingsFeedback.SAVED),
            )
        }
    }

    private fun Result<Unit>.toFeedback(success: SettingsFeedback): SettingsFeedback = when (this) {
        is Result.Success -> success
        is Result.Error -> when (message) {
            "invalid_url" -> SettingsFeedback.INVALID_URL
            "token_required" -> SettingsFeedback.TOKEN_REQUIRED
            else -> SettingsFeedback.CONNECTION_FAILED
        }
    }
}
