/* Presentation-layer state contract for the Settings screen. */
package com.teja.finfly.presentation.settings

import java.time.Instant

data class SettingsForm(
    val serverUrl: String = "",
    val bearerToken: String = "",
    val lastSyncTime: Instant? = null,
    val showToken: Boolean = false,
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val feedback: SettingsFeedback? = null,
)

enum class SettingsFeedback { CONNECTION_SUCCESS, CONNECTION_FAILED, SAVED, INVALID_URL, TOKEN_REQUIRED }

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val form: SettingsForm) : SettingsUiState
    data object Empty : SettingsUiState
    data object Error : SettingsUiState
}
