/* Presentation-layer state contract for filtered cached financial reports. */
package com.teja.finflyiii.presentation.reports

import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.ReportsFilter
import com.teja.finflyiii.domain.model.ReportsSummary
import com.teja.finflyiii.domain.model.Tag

data class ReportsFilterForm(
    val fromDate: String,
    val untilDate: String,
    val selectedCategories: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val appliedFilter: ReportsFilter,
    val error: ReportsFilterError? = null,
) {
    val activeCount: Int get() = 1 + appliedFilter.categories.size + appliedFilter.tags.size
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
