/* Presentation-layer state contract for cached financial reports. */
package com.teja.finfly.presentation.reports

import com.teja.finfly.domain.model.ReportsSummary

sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data class Success(val summary: ReportsSummary, val isRefreshing: Boolean) : ReportsUiState
    data class Empty(val isRefreshing: Boolean) : ReportsUiState
    data object Error : ReportsUiState
}
