/* Presentation-layer state contract for the Dashboard screen. */
package com.teja.finfly.presentation.dashboard

import com.teja.finfly.domain.model.DashboardSummary

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val summary: DashboardSummary,
        val isRefreshing: Boolean,
        val showNetWorthSummary: Boolean,
    ) : DashboardUiState
    data class Empty(val isRefreshing: Boolean, val refreshFailed: Boolean = false) : DashboardUiState
    data class Error(val retryable: Boolean = true) : DashboardUiState
}
