/* Presentation-layer state contract for the filterable Transactions screen. */
package com.teja.finfly.presentation.transactions

import com.teja.finfly.domain.model.Category
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionFilter

/** Immutable transaction timeline content, filter options, and loading feedback. */
data class TransactionsUiState(
    val isLoading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val hasMore: Boolean = false,
    val filter: TransactionFilter = TransactionFilter(),
    val categories: List<Category> = emptyList(),
    val hasError: Boolean = false,
)
