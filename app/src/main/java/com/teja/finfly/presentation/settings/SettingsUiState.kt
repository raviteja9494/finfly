/* Presentation-layer state contract for the Settings screen. */
package com.teja.finfly.presentation.settings

import com.teja.finfly.domain.model.DashboardChartPeriod
import com.teja.finfly.domain.model.DashboardRangeMode
import com.teja.finfly.domain.model.CategoryChartStyle
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
    val useDeviceTimezone: Boolean = true,
)

enum class SettingsFeedback { CONNECTION_SUCCESS, CONNECTION_FAILED, SAVED, LOGGED_OUT, INVALID_URL, TOKEN_REQUIRED }

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val form: SettingsForm) : SettingsUiState
    data object Empty : SettingsUiState
    data object Error : SettingsUiState
}
