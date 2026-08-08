/* Presentation-layer state for creating or editing a transaction group. */
package com.teja.finflyiii.presentation.transactioneditor

import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.FireflyFeatureItem
import com.teja.finflyiii.domain.model.Tag
import com.teja.finflyiii.domain.model.TransactionType
import java.time.Instant

enum class TransactionEditorError {
    REQUIRED_FIELDS, INVALID_AMOUNT, INVALID_DATE, INVALID_CURRENCY, SAVE_FAILED, LOAD_FAILED
}

/** One editable Firefly journal inside a transaction group. */
data class TransactionSplitUiState(
    val key: Long,
    val localId: String? = null,
    val journalId: String? = null,
    val amount: String = "",
    val description: String = "",
    val sourceAccountId: String? = null,
    val sourceAccount: String = "",
    val destinationAccountId: String? = null,
    val destinationAccount: String = "",
    val category: String = "",
    val budget: String = "",
    val selectedTags: Set<String> = emptySet(),
    val notes: String = "",
)

/** Shared group values, split rows, selector options, and operation feedback. */
data class TransactionEditorUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val type: TransactionType = TransactionType.WITHDRAWAL,
    val date: Instant,
    val dateText: String = "",
    val currency: String = "",
    val splits: List<TransactionSplitUiState> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val budgets: List<FireflyFeatureItem> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: TransactionEditorError? = null,
    val errorDetails: String? = null,
) {
    val totalAmount: java.math.BigDecimal
        get() = splits.fold(java.math.BigDecimal.ZERO) { total, split ->
            total + (split.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO)
        }
}
