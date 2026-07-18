/* Presentation-layer state contract for the Transactions screen. */
package com.teja.finfly.presentation.transactions

import com.teja.finfly.domain.model.Transaction

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data class Success(val transactions: List<Transaction>, val isLoadingMore: Boolean) : TransactionsUiState
    data object Empty : TransactionsUiState
    data object Error : TransactionsUiState
}
