/* Presentation-layer state contract for filtered cached financial reports. */
package com.teja.finfly.presentation.reports

import com.teja.finfly.domain.model.Category
import com.teja.finfly.domain.model.ReportsFilter
import com.teja.finfly.domain.model.ReportsSummary
import com.teja.finfly.domain.model.Tag

data class ReportsFilterForm(
    val fromDate: String,
    val untilDate: String,
    val category: String? = null,
    val tag: String? = null,
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val appliedFilter: ReportsFilter,
    val error: ReportsFilterError? = null,
) {
    val activeCount: Int get() = 1 + listOfNotNull(appliedFilter.category, appliedFilter.tag).size
}

enum class ReportsFilterError { INVALID_DATE, INVALID_RANGE, RANGE_TOO_LARGE }

sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data class Success(
        val summary: ReportsSummary,
        val isRefreshing: Boolean,
        val filterForm: ReportsFilterForm,
    ) : ReportsUiState
    data class Empty(val isRefreshing: Boolean, val filterForm: ReportsFilterForm) : ReportsUiState
    data object Error : ReportsUiState
}
