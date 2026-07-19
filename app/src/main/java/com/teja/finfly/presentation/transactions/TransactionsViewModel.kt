/* Presentation-layer ViewModel managing the filtered Room-backed transaction list. */
package com.teja.finfly.presentation.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Category
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.domain.model.Tag
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.domain.repository.TagRepository
import com.teja.finfly.domain.usecase.ObserveTransactionsUseCase
import com.teja.finfly.domain.usecase.ObserveTransactionCountUseCase
import com.teja.finfly.domain.usecase.SyncFinancesUseCase
import com.teja.finfly.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import java.time.Instant
import javax.inject.Inject

/** Owns paging, debounced search, and AND-combined filters while preserving route-provided bounds. */
@HiltViewModel
@OptIn(FlowPreview::class)
class TransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTransactions: ObserveTransactionsUseCase,
    private val observeTransactionCount: ObserveTransactionCountUseCase,
    transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
    private val syncFinances: SyncFinancesUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AppRoute.Transactions>()
    private val baseFilter: TransactionFilter = TransactionFilter(
        accountIds = route.accountId?.let(::setOf).orEmpty(),
        categories = route.categories.split(REPORT_FILTER_SEPARATOR).filter(String::isNotBlank).toSet(),
        tags = route.tags.split(REPORT_FILTER_SEPARATOR).filter(String::isNotBlank).toSet(),
        from = route.fromEpochMillis?.let(Instant::ofEpochMilli),
        until = route.untilEpochMillis?.let(Instant::ofEpochMilli),
    )
    private val requestedCount = MutableStateFlow(PAGE_SIZE)
    private val filter: MutableStateFlow<TransactionFilter> = MutableStateFlow(baseFilter)
    private val searchQuery = MutableStateFlow(baseFilter.query)
    private val categories: Flow<List<Category>> = transactionRepository.observeCategories().map { result ->
        when (result) {
            is Result.Success -> result.value
            is Result.Error -> emptyList()
        }
    }
    private val tags: Flow<List<Tag>> = tagRepository.observeTags().map { result ->
        when (result) {
            is Result.Success -> result.value
            is Result.Error -> emptyList()
        }
    }

    private val contentState = combine(requestedCount, filter, categories, tags) { count, criteria, categories, tags ->
        FilterData(count, criteria, categories, tags)
    }.flatMapLatest { data ->
        val count = data.count
        val criteria = data.filter
        combine(
            observeTransactions(criteria, count, 0),
            observeTransactionCount(criteria),
        ) { result, countResult ->
            when {
                result is Result.Error || countResult is Result.Error -> TransactionsUiState(
                    isLoading = false,
                    filter = criteria,
                    categories = data.categories,
                    tags = data.tags,
                    hasError = true,
                )
                result is Result.Success && countResult is Result.Success -> TransactionsUiState(
                    isLoading = false,
                    transactions = result.value,
                    resultCount = countResult.value,
                    hasMore = result.value.size < countResult.value,
                    filter = criteria,
                    categories = data.categories,
                    tags = data.tags,
                )
                else -> TransactionsUiState(isLoading = false, hasError = true)
            }
        }
    }

    val uiState = combine(contentState, searchQuery) { state, query ->
        state.copy(searchQuery = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    init {
        viewModelScope.launch {
            searchQuery.debounce(SEARCH_DEBOUNCE_MILLIS).distinctUntilChanged().collect { query ->
                updateFilter { copy(query = query) }
            }
        }
        viewModelScope.launch { tagRepository.refresh() }
    }

    fun setQuery(value: String) {
        searchQuery.value = value
    }

    fun setType(value: TransactionType?) = updateFilter {
        copy(types = value?.let(::setOf).orEmpty())
    }

    fun setCategory(value: String?) = updateFilter {
        copy(categories = value?.let(::setOf).orEmpty())
    }

    fun setTag(value: String?) = updateFilter {
        copy(tags = value?.let(::setOf).orEmpty())
    }

    fun clearFilters() {
        requestedCount.value = PAGE_SIZE
        filter.value = baseFilter.copy(query = searchQuery.value)
    }

    fun retry() {
        viewModelScope.launch { syncFinances() }
    }

    fun loadMore() {
        if (uiState.value.hasMore) requestedCount.value += PAGE_SIZE
    }

    private fun updateFilter(transform: TransactionFilter.() -> TransactionFilter) {
        requestedCount.value = PAGE_SIZE
        filter.value = filter.value.transform()
    }

    private data class FilterData(
        val count: Int,
        val filter: TransactionFilter,
        val categories: List<Category>,
        val tags: List<com.teja.finfly.domain.model.Tag>,
    )

    private companion object {
        const val PAGE_SIZE = 25
        const val SEARCH_DEBOUNCE_MILLIS = 300L
        const val REPORT_FILTER_SEPARATOR = "\u001F"
    }
}
