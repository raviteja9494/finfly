/* Presentation-layer state contract for the filterable Transactions screen. */
package com.teja.finflyiii.presentation.transactions

import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.TransactionFilter
import com.teja.finflyiii.domain.model.Tag

/** Immutable transaction timeline content, filter options, and loading feedback. */
data class TransactionsUiState(
    val isLoading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val resultCount: Int = 0,
    val hasMore: Boolean = false,
    val searchQuery: String = "",
    val filter: TransactionFilter = TransactionFilter(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val hasError: Boolean = false,
) {
    val activeFilterCount: Int
        get() = filter.categories.size + filter.types.size + filter.tags.size
}
