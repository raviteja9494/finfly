/* Presentation-layer ViewModel managing Dashboard aggregates and shared sync state. */
package com.teja.finflyiii.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.SyncState
import com.teja.finflyiii.domain.repository.SettingsRepository
import com.teja.finflyiii.domain.usecase.ObserveDashboardUseCase
import com.teja.finflyiii.domain.usecase.SyncFinancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** Combines cached Dashboard content with the application-wide synchronization lifecycle. */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    private val syncFinances: SyncFinancesUseCase,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val dashboard = settingsRepository.settings.flatMapLatest { settings ->
        observeDashboard(
            settings.recentTransactionsCount,
            settings.dashboardChartPeriod,
            settings.dashboardRangeMode,
            settings.categoryChartPeriod,
            settings.categoryRangeMode,
        ).map { result -> result to settings }
    }

    val uiState = combine(dashboard, syncFinances.state) { (result, settings), syncState ->
        when (result) {
            is Result.Error -> DashboardUiState.Error()
            is Result.Success -> {
                val summary = result.value
                val refreshing = syncState is SyncState.Syncing
                val failed = syncState is SyncState.Error
                if (summary.recentTransactions.isEmpty() &&
                    summary.todaySpend.compareTo(BigDecimal.ZERO) == 0 &&
                    summary.monthSpend.compareTo(BigDecimal.ZERO) == 0
                ) DashboardUiState.Empty(refreshing, failed)
                else DashboardUiState.Success(
                    summary,
                    refreshing,
                    settings.showNetWorthSummary,
                    settings.showSpendingInsight,
                    settings.categoryChartStyle,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .filter {
                    it.serverUrl.isNotBlank() && it.bearerToken.isNotBlank() && it.lastSyncTime == null
                }
                .take(1)
                .collect { syncFinances() }
        }
    }

    fun refresh() {
        viewModelScope.launch { syncFinances() }
    }
}
