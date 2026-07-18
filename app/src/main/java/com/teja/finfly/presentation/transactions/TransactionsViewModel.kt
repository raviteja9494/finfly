/* Presentation-layer ViewModel managing the filtered Room-backed transaction list. */
package com.teja.finfly.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.Category
import com.teja.finfly.domain.model.Tag
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.domain.usecase.ObserveTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Owns paging and filter selections while exposing cached filter metadata. */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val observeTransactions: ObserveTransactionsUseCase,
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
) : ViewModel() {
    private val requestedCount = MutableStateFlow(PAGE_SIZE)
    private val filter = MutableStateFlow(TransactionFilter())
    private val options = combine(
        transactionRepository.observeCategories(),
        transactionRepository.observeTags(),
        accountRepository.observeAccounts(),
    ) { categories, tags, accounts ->
        FilterOptions(
            categories = when (categories) {
                is Result.Success -> categories.value
                is Result.Error -> emptyList()
            },
            tags = when (tags) {
                is Result.Success -> tags.value
                is Result.Error -> emptyList()
            },
            accounts = when (accounts) {
                is Result.Success -> accounts.value
                is Result.Error -> emptyList()
            },
        )
    }

    val uiState = combine(requestedCount, filter, options) { count, criteria, available ->
        Triple(count, criteria, available)
    }.flatMapLatest { (count, criteria, available) ->
        observeTransactions(criteria, count, 0).map { result ->
            when (result) {
                is Result.Error -> TransactionsUiState(
                    isLoading = false,
                    filter = criteria,
                    categories = available.categories,
                    tags = available.tags,
                    accounts = available.accounts,
                    hasError = true,
                )
                is Result.Success -> TransactionsUiState(
                    isLoading = false,
                    transactions = result.value,
                    hasMore = result.value.size >= count,
                    filter = criteria,
                    categories = available.categories,
                    tags = available.tags,
                    accounts = available.accounts,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setQuery(value: String) = updateFilter { copy(query = value) }

    fun toggleType(value: TransactionType) = updateFilter {
        copy(types = types.toggle(value))
    }

    fun toggleCategory(value: String) = updateFilter {
        copy(categories = categories.toggle(value))
    }

    fun toggleTag(value: String) = updateFilter {
        copy(tags = tags.toggle(value))
    }

    fun toggleAccount(value: String) = updateFilter {
        copy(accountIds = accountIds.toggle(value))
    }

    fun clearFilters() {
        requestedCount.value = PAGE_SIZE
        filter.value = TransactionFilter()
    }

    fun loadMore() {
        if (uiState.value.hasMore) requestedCount.value += PAGE_SIZE
    }

    private fun updateFilter(transform: TransactionFilter.() -> TransactionFilter) {
        requestedCount.value = PAGE_SIZE
        filter.value = filter.value.transform()
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

    private data class FilterOptions(
        val categories: List<Category>,
        val tags: List<Tag>,
        val accounts: List<Account>,
    )

    private companion object { const val PAGE_SIZE = 25 }
}
