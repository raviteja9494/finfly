/* Presentation-layer ViewModel managing Dashboard aggregates and shared sync state. */
package com.teja.finfly.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.SyncState
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.usecase.ObserveDashboardUseCase
import com.teja.finfly.domain.usecase.SyncFinancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
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
    val uiState = combine(observeDashboard(), syncFinances.state) { result, syncState ->
        when (result) {
            is Result.Error -> DashboardUiState.Error()
            is Result.Success -> {
                val summary = result.value
                val refreshing = syncState is SyncState.Syncing
                val failed = syncState is SyncState.Error
                if (summary.recentTransactions.isEmpty() &&
                    summary.accounts.isEmpty() &&
                    summary.todaySpend.compareTo(BigDecimal.ZERO) == 0 &&
                    summary.monthSpend.compareTo(BigDecimal.ZERO) == 0
                ) DashboardUiState.Empty(refreshing, failed)
                else DashboardUiState.Success(summary, refreshing)
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
