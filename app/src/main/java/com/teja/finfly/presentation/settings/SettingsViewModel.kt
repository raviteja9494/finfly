/* Presentation-layer ViewModel managing the Firefly connection settings form. */
package com.teja.finfly.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.usecase.SaveSettingsUseCase
import com.teja.finfly.domain.usecase.TestConnectionUseCase
import com.teja.finfly.domain.usecase.SyncFinancesUseCase
import com.teja.finfly.domain.usecase.SaveDashboardPreferencesUseCase
import com.teja.finfly.domain.model.DashboardChartPeriod
import com.teja.finfly.domain.model.DashboardRangeMode
import com.teja.finfly.domain.model.CategoryChartStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Manages credentials, Dashboard preferences, persistence progress, and connection-test feedback. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val saveSettings: SaveSettingsUseCase,
    private val testConnection: TestConnectionUseCase,
    private val syncFinances: SyncFinancesUseCase,
    private val saveDashboardPreferences: SaveDashboardPreferencesUseCase,
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
                    showNetWorthSummary = saved.showNetWorthSummary,
                    recentTransactionsCount = saved.recentTransactionsCount,
                    dashboardChartPeriod = saved.dashboardChartPeriod,
                    dashboardRangeMode = saved.dashboardRangeMode,
                    showSpendingInsight = saved.showSpendingInsight,
                    categoryChartStyle = saved.categoryChartStyle,
                )
            }
        }
    }

    fun updateServerUrl(value: String) { form.value = form.value.copy(serverUrl = value, feedback = null) }
    fun updateBearerToken(value: String) { form.value = form.value.copy(bearerToken = value, feedback = null) }
    fun toggleTokenVisibility() { form.value = form.value.copy(showToken = !form.value.showToken) }
    fun setShowNetWorthSummary(value: Boolean) {
        form.value = form.value.copy(showNetWorthSummary = value)
        persistDashboardPreferences()
    }
    fun setRecentTransactionsCount(value: Int) {
        form.value = form.value.copy(recentTransactionsCount = value)
        persistDashboardPreferences()
    }
    fun setDashboardChartPeriod(value: DashboardChartPeriod) {
        form.value = form.value.copy(dashboardChartPeriod = value)
        persistDashboardPreferences()
    }
    fun setDashboardRangeMode(value: DashboardRangeMode) {
        form.value = form.value.copy(dashboardRangeMode = value)
        persistDashboardPreferences()
    }
    fun setShowSpendingInsight(value: Boolean) {
        form.value = form.value.copy(showSpendingInsight = value)
        persistDashboardPreferences()
    }
    fun setCategoryChartStyle(value: CategoryChartStyle) {
        form.value = form.value.copy(categoryChartStyle = value)
        persistDashboardPreferences()
    }

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
            if (result is Result.Success) {
                settingsRepository.settings.first {
                    it.serverUrl.isNotBlank() && it.bearerToken.isNotBlank()
                }
                syncFinances()
            }
        }
    }

    private fun persistDashboardPreferences() {
        val preferences = form.value
        viewModelScope.launch {
            saveDashboardPreferences(
                preferences.showNetWorthSummary,
                preferences.recentTransactionsCount,
                preferences.dashboardChartPeriod,
                preferences.dashboardRangeMode,
                preferences.showSpendingInsight,
                preferences.categoryChartStyle,
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
