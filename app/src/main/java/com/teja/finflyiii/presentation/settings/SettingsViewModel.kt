/* Presentation-layer ViewModel managing the Firefly connection settings form. */
package com.teja.finflyiii.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.repository.SettingsRepository
import com.teja.finflyiii.domain.repository.AiRepository
import com.teja.finflyiii.domain.repository.AiSettingsRepository
import com.teja.finflyiii.domain.assistant.FinanceAssistant
import com.teja.finflyiii.domain.model.AiConfig
import com.teja.finflyiii.domain.usecase.SaveSettingsUseCase
import com.teja.finflyiii.domain.usecase.TestConnectionUseCase
import com.teja.finflyiii.domain.usecase.SyncFinancesUseCase
import com.teja.finflyiii.domain.usecase.SaveDashboardPreferencesUseCase
import com.teja.finflyiii.domain.model.DashboardChartPeriod
import com.teja.finflyiii.domain.model.DashboardRangeMode
import com.teja.finflyiii.domain.model.CategoryChartStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

/** Manages credentials, Dashboard preferences, persistence progress, and connection-test feedback. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val saveSettings: SaveSettingsUseCase,
    private val testConnection: TestConnectionUseCase,
    private val syncFinances: SyncFinancesUseCase,
    private val saveDashboardPreferences: SaveDashboardPreferencesUseCase,
    private val aiRepository: AiRepository,
    private val aiSettingsRepository: AiSettingsRepository,
    private val financeAssistant: FinanceAssistant,
) : ViewModel() {
    private val form = MutableStateFlow(SettingsForm())
    private var aiDownloadJob: Job? = null

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
                    categoryChartPeriod = saved.categoryChartPeriod,
                    categoryRangeMode = saved.categoryRangeMode,
                    useDeviceTimezone = saved.useDeviceTimezone,
                )
            }
        }
        viewModelScope.launch {
            aiSettingsRepository.config.collect { config -> form.value = form.value.copy(aiConfig = config) }
        }
        viewModelScope.launch {
            aiRepository.getModelState().collect { state -> form.value = form.value.copy(aiModelState = state) }
        }
        viewModelScope.launch {
            aiSettingsRepository.huggingFaceToken.collect { token ->
                form.value = form.value.copy(huggingFaceToken = token)
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
    fun setCategoryChartPeriod(value: DashboardChartPeriod) {
        form.value = form.value.copy(categoryChartPeriod = value)
        persistDashboardPreferences()
    }
    fun setCategoryRangeMode(value: DashboardRangeMode) {
        form.value = form.value.copy(categoryRangeMode = value)
        persistDashboardPreferences()
    }
    fun setUseDeviceTimezone(value: Boolean) {
        form.value = form.value.copy(useDeviceTimezone = value)
        viewModelScope.launch { settingsRepository.setUseDeviceTimezone(value) }
    }

    fun setAiMaxTransactions(value: Int) = updateAiConfig { copy(maxTransactions = value.coerceIn(10, 100)) }
    fun setAiDateRangeDays(value: Int) = updateAiConfig { copy(dateRangeDays = value) }
    fun setAiIncludeBalances(value: Boolean) = updateAiConfig { copy(includeBalances = value) }
    fun setAiIncludeCategories(value: Boolean) = updateAiConfig { copy(includeCategories = value) }
    fun setAiIncludeSmsRules(value: Boolean) = updateAiConfig { copy(includeSmsRules = value) }
    fun setAiTemperature(value: Float) = updateAiConfig { copy(temperature = value.coerceIn(0.1f, 1f)) }
    fun setAiMaxResponseTokens(value: Int) = updateAiConfig { copy(maxResponseTokens = value) }
    fun updateHuggingFaceToken(value: String) {
        form.value = form.value.copy(huggingFaceToken = value)
        viewModelScope.launch { aiSettingsRepository.saveHuggingFaceToken(value) }
    }
    fun toggleHuggingFaceTokenVisibility() {
        form.value = form.value.copy(showHuggingFaceToken = !form.value.showHuggingFaceToken)
    }

    fun downloadAiModel() {
        if (aiDownloadJob?.isActive == true) return
        aiDownloadJob = viewModelScope.launch {
            aiRepository.downloadModel().collect { state ->
                form.value = form.value.copy(aiModelState = state)
                if (state is com.teja.finflyiii.domain.model.AiModelState.Downloaded) {
                    form.value = form.value.copy(aiModelState = com.teja.finflyiii.domain.model.AiModelState.Loading)
                    runCatching { financeAssistant.prepare() }.fold(
                        onSuccess = {
                            form.value = form.value.copy(
                                aiModelState = com.teja.finflyiii.domain.model.AiModelState.Ready
                            )
                        },
                        onFailure = {
                            form.value = form.value.copy(
                                aiModelState = com.teja.finflyiii.domain.model.AiModelState.Error("initialization")
                            )
                        },
                    )
                }
            }
        }
    }

    fun cancelAiDownload() {
        aiDownloadJob?.cancel()
        aiDownloadJob = null
    }

    fun deleteAiModel() {
        viewModelScope.launch {
            financeAssistant.reset()
            aiRepository.deleteModel()
        }
    }

    fun importAiModel(sourceUri: String) {
        if (aiDownloadJob?.isActive == true) return
        aiDownloadJob = viewModelScope.launch {
            financeAssistant.reset()
            form.value = form.value.copy(
                aiModelState = com.teja.finflyiii.domain.model.AiModelState.Loading,
                aiFileFeedback = null,
            )
            when (val result = aiRepository.importModel(sourceUri)) {
                is Result.Success -> runCatching { financeAssistant.prepare() }.fold(
                    onSuccess = {
                        form.value = form.value.copy(
                            aiModelState = com.teja.finflyiii.domain.model.AiModelState.Ready,
                            aiFileFeedback = AiFileFeedback.IMPORTED,
                        )
                    },
                    onFailure = {
                        form.value = form.value.copy(
                            aiModelState = com.teja.finflyiii.domain.model.AiModelState.Error("initialization"),
                            aiFileFeedback = AiFileFeedback.FAILED,
                        )
                    },
                )
                is Result.Error -> form.value = form.value.copy(
                    aiFileFeedback = if (result.message == "ai_model_import_invalid") {
                        AiFileFeedback.INVALID_MODEL
                    } else AiFileFeedback.FAILED,
                )
            }
        }
    }

    fun exportAiModel(destinationUri: String) {
        viewModelScope.launch {
            form.value = form.value.copy(aiFileFeedback = null)
            form.value = form.value.copy(
                aiFileFeedback = when (aiRepository.exportModel(destinationUri)) {
                    is Result.Success -> AiFileFeedback.EXPORTED
                    is Result.Error -> AiFileFeedback.FAILED
                }
            )
        }
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

    fun logout() {
        if (form.value.isLoggingOut) return
        viewModelScope.launch {
            form.value = form.value.copy(isLoggingOut = true, feedback = null)
            val result = settingsRepository.logout()
            form.value = form.value.copy(
                isLoggingOut = false,
                showToken = false,
                feedback = result.toFeedback(success = SettingsFeedback.LOGGED_OUT),
            )
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
                preferences.categoryChartPeriod,
                preferences.categoryRangeMode,
            )
        }
    }

    private fun updateAiConfig(transform: AiConfig.() -> AiConfig) {
        val updated = form.value.aiConfig.transform()
        form.value = form.value.copy(aiConfig = updated)
        viewModelScope.launch { aiSettingsRepository.saveConfig(updated) }
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
