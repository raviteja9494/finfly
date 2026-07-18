/* Presentation-layer ViewModel managing dashboard aggregates and pull-to-refresh state. */
package com.teja.finfly.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.usecase.ObserveDashboardUseCase
import com.teja.finfly.domain.usecase.SyncFinancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** Manages cached dashboard content, automatic first sync, and manual refresh feedback. */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    private val syncFinances: SyncFinancesUseCase,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    private val refreshFailed = MutableStateFlow(false)

    val uiState = combine(observeDashboard(), refreshing, refreshFailed) { result, isRefreshing, failed ->
        when (result) {
            is Result.Error -> DashboardUiState.Error()
            is Result.Success -> {
                val summary = result.value
                if (summary.recentTransactions.isEmpty() &&
                    summary.accounts.isEmpty() &&
                    summary.todaySpend.compareTo(BigDecimal.ZERO) == 0 &&
                    summary.monthSpend.compareTo(BigDecimal.ZERO) == 0
                ) DashboardUiState.Empty(isRefreshing, failed)
                else DashboardUiState.Success(summary, isRefreshing)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.serverUrl.isNotBlank() && it.bearerToken.isNotBlank() }
                .distinctUntilChanged()
                .filter { it }
                .take(1)
                .collect { refresh() }
        }
    }

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            refreshFailed.value = syncFinances() is Result.Error
            refreshing.value = false
        }
    }
}
