/* Presentation-layer ViewModel for filtered cached Reports. */
package com.teja.finfly.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.ReportsFilter
import com.teja.finfly.domain.model.SyncState
import com.teja.finfly.domain.repository.TagRepository
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.domain.usecase.ObserveReportsUseCase
import com.teja.finfly.domain.usecase.SyncFinancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Applies validated date/category/tag filters and combines report data with shared synchronization state. */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    observeReports: ObserveReportsUseCase,
    transactionRepository: TransactionRepository,
    tagRepository: TagRepository,
    private val syncFinances: SyncFinancesUseCase,
    clock: Clock,
) : ViewModel() {
    private val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
    private val defaultFilter = ReportsFilter(today.withDayOfMonth(1), today)
    private val appliedFilter = MutableStateFlow(defaultFilter)
    private val filterForm = MutableStateFlow(defaultFilter.toForm())
    private val report = appliedFilter.flatMapLatest(observeReports::invoke)
    private val choices = combine(
        transactionRepository.observeCategories().map { it.valuesOrEmpty() },
        tagRepository.observeTags().map { it.valuesOrEmpty() },
    ) { categories, tags -> categories to tags }

    val uiState = combine(report, choices, filterForm, syncFinances.state) { result, choices, form, syncState ->
        val completeForm = form.copy(categories = choices.first, tags = choices.second)
        when (result) {
            is Result.Error -> ReportsUiState.Error
            is Result.Success -> if (result.value.transactionCount == 0) {
                ReportsUiState.Empty(syncState is SyncState.Syncing, completeForm)
            } else {
                ReportsUiState.Success(result.value, syncState is SyncState.Syncing, completeForm)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState.Loading)

    fun setFromDate(value: String) = updateForm { copy(fromDate = value, error = null) }
    fun setUntilDate(value: String) = updateForm { copy(untilDate = value, error = null) }
    fun toggleCategory(value: String) = updateForm {
        copy(
            selectedCategories = if (value in selectedCategories) selectedCategories - value else selectedCategories + value,
            error = null,
        )
    }
    fun toggleTag(value: String) = updateForm {
        copy(selectedTags = if (value in selectedTags) selectedTags - value else selectedTags + value, error = null)
    }
    fun clearCategories() = updateForm { copy(selectedCategories = emptySet(), error = null) }
    fun clearTags() = updateForm { copy(selectedTags = emptySet(), error = null) }

    fun applyFilters() {
        val form = filterForm.value
        val from = runCatching { LocalDate.parse(form.fromDate) }.getOrNull()
        val until = runCatching { LocalDate.parse(form.untilDate) }.getOrNull()
        val error = when {
            from == null || until == null -> ReportsFilterError.INVALID_DATE
            until < from -> ReportsFilterError.INVALID_RANGE
            ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(until)) >= MAX_REPORT_MONTHS ->
                ReportsFilterError.RANGE_TOO_LARGE
            else -> null
        }
        if (error != null) {
            updateForm { copy(error = error) }
            return
        }
        val filter = ReportsFilter(from!!, until!!, form.selectedCategories, form.selectedTags)
        filterForm.value = form.copy(appliedFilter = filter, error = null)
        appliedFilter.value = filter
        syncFilterRange(filter)
    }

    fun clearFilters() {
        filterForm.value = defaultFilter.toForm()
        appliedFilter.value = defaultFilter
    }

    fun refresh() {
        syncFilterRange(appliedFilter.value)
    }

    private fun syncFilterRange(filter: ReportsFilter) {
        val zone = ZoneId.systemDefault()
        val from = filter.fromDate.atStartOfDay(zone).toInstant()
        val until = filter.untilDate.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1)
        viewModelScope.launch { syncFinances(from, until) }
    }

    private fun updateForm(transform: ReportsFilterForm.() -> ReportsFilterForm) {
        filterForm.value = filterForm.value.transform()
    }

    private fun ReportsFilter.toForm() = ReportsFilterForm(
        fromDate = fromDate.toString(),
        untilDate = untilDate.toString(),
        selectedCategories = categories,
        selectedTags = tags,
        appliedFilter = this,
    )

    private fun <T> Result<List<T>>.valuesOrEmpty(): List<T> = (this as? Result.Success)?.value.orEmpty()

    private companion object {
        const val MAX_REPORT_MONTHS = 12L
    }
}
