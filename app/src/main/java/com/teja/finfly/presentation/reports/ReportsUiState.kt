/* Presentation-layer state contract reserved for the Phase 3 Reports screen. */
package com.teja.finfly.presentation.reports

sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data object Success : ReportsUiState
    data object Empty : ReportsUiState
    data object Error : ReportsUiState
}
