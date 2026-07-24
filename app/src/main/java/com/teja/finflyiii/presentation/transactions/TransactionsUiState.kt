/* Presentation-layer state contract for the filterable Transactions screen. */
package com.teja.finflyiii.presentation.transactions

import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.TransactionFilter
import com.teja.finflyiii.domain.model.Tag
import java.time.ZoneId

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
    private val zone: ZoneId get() = ZoneId.systemDefault()
    val fromDate: String
        get() = filter.from?.atZone(zone)?.toLocalDate()?.toString().orEmpty()
    val untilDate: String
        get() = filter.until?.minusNanos(1)?.atZone(zone)?.toLocalDate()?.toString().orEmpty()
    val activeFilterCount: Int
        get() = filter.categories.size + filter.types.size + filter.tags.size +
            if (filter.from != null || filter.until != null) 1 else 0
}
