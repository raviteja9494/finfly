/* Presentation-layer state contract for the Transaction Detail screen. */
package com.teja.finflyiii.presentation.transactiondetail

import com.teja.finflyiii.domain.model.Transaction

/** Loading, content, missing, and failure outcomes for one cached transaction. */
sealed interface TransactionDetailUiState {
    data object Loading : TransactionDetailUiState
    data class Success(
        val transaction: Transaction,
        val reference: String?,
        val displayNotes: String?,
        val rawSms: String?,
    ) : TransactionDetailUiState
    data object Empty : TransactionDetailUiState
    data object Error : TransactionDetailUiState
}

data class TransactionDeletionState(
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val failed: Boolean = false,
)
