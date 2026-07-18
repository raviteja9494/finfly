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
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.domain.usecase.ObserveTransactionsUseCase
import com.teja.finfly.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

/** Owns paging plus one category/type selection while preserving route-provided date/account bounds. */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTransactions: ObserveTransactionsUseCase,
    transactionRepository: TransactionRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AppRoute.Transactions>()
    private val baseFilter = TransactionFilter(
        accountIds = route.accountId?.let(::setOf).orEmpty(),
        from = route.fromEpochMillis?.let(Instant::ofEpochMilli),
        until = route.untilEpochMillis?.let(Instant::ofEpochMilli),
    )
    private val requestedCount = MutableStateFlow(PAGE_SIZE)
    private val filter = MutableStateFlow(baseFilter)
    private val categories = transactionRepository.observeCategories().map { result ->
        when (result) {
            is Result.Success -> result.value
            is Result.Error -> emptyList()
        }
    }

    val uiState = combine(requestedCount, filter, categories) { count, criteria, available ->
        Triple(count, criteria, available)
    }.flatMapLatest { (count, criteria, available) ->
        observeTransactions(criteria, count, 0).map { result ->
            when (result) {
                is Result.Error -> TransactionsUiState(
                    isLoading = false,
                    filter = criteria,
                    categories = available,
                    hasError = true,
                )
                is Result.Success -> TransactionsUiState(
                    isLoading = false,
                    transactions = result.value,
                    hasMore = result.value.size >= count,
                    filter = criteria,
                    categories = available,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setQuery(value: String) = updateFilter { copy(query = value) }

    fun setType(value: TransactionType?) = updateFilter {
        copy(types = value?.let(::setOf).orEmpty())
    }

    fun setCategory(value: String?) = updateFilter {
        copy(categories = value?.let(::setOf).orEmpty())
    }

    fun clearFilters() {
        requestedCount.value = PAGE_SIZE
        filter.value = baseFilter
    }

    fun loadMore() {
        if (uiState.value.hasMore) requestedCount.value += PAGE_SIZE
    }

    private fun updateFilter(transform: TransactionFilter.() -> TransactionFilter) {
        requestedCount.value = PAGE_SIZE
        filter.value = filter.value.transform()
    }

    private companion object { const val PAGE_SIZE = 25 }
}
