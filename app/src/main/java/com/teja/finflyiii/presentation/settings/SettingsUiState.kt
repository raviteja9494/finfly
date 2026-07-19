/* Presentation-layer state contract for the Settings screen. */
package com.teja.finflyiii.presentation.settings

import com.teja.finflyiii.domain.model.DashboardChartPeriod
import com.teja.finflyiii.domain.model.DashboardRangeMode
import com.teja.finflyiii.domain.model.CategoryChartStyle
import com.teja.finflyiii.domain.model.AiConfig
import com.teja.finflyiii.domain.model.AiModelState
import java.time.Instant

data class SettingsForm(
    val serverUrl: String = "",
    val bearerToken: String = "",
    val lastSyncTime: Instant? = null,
    val showToken: Boolean = false,
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val isLoggingOut: Boolean = false,
    val feedback: SettingsFeedback? = null,
    val showNetWorthSummary: Boolean = false,
    val recentTransactionsCount: Int = 10,
    val dashboardChartPeriod: DashboardChartPeriod = DashboardChartPeriod.WEEK,
    val dashboardRangeMode: DashboardRangeMode = DashboardRangeMode.CALENDAR,
    val showSpendingInsight: Boolean = true,
    val categoryChartStyle: CategoryChartStyle = CategoryChartStyle.BARS,
    val categoryChartPeriod: DashboardChartPeriod = DashboardChartPeriod.MONTH,
    val categoryRangeMode: DashboardRangeMode = DashboardRangeMode.CALENDAR,
    val useDeviceTimezone: Boolean = true,
    val aiConfig: AiConfig = AiConfig(),
    val aiModelState: AiModelState = AiModelState.NotDownloaded(0),
    val huggingFaceToken: String = "",
    val showHuggingFaceToken: Boolean = false,
    val aiFileFeedback: AiFileFeedback? = null,
)

enum class AiFileFeedback { IMPORTED, EXPORTED, INVALID_MODEL, FAILED }

enum class SettingsFeedback { CONNECTION_SUCCESS, CONNECTION_FAILED, SAVED, LOGGED_OUT, INVALID_URL, TOKEN_REQUIRED }

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val form: SettingsForm) : SettingsUiState
    data object Empty : SettingsUiState
    data object Error : SettingsUiState
}
