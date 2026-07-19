/* Presentation-layer state for creating or editing one transaction. */
package com.teja.finflyiii.presentation.transactioneditor

import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.Tag
import com.teja.finflyiii.domain.model.FireflyFeatureItem
import com.teja.finflyiii.domain.model.TransactionType
import java.time.Instant

enum class TransactionEditorError { REQUIRED_FIELDS, INVALID_AMOUNT, INVALID_DATE, INVALID_CURRENCY, SAVE_FAILED, LOAD_FAILED }

/** Form values, selector options, and operation feedback for the transaction editor. */
data class TransactionEditorUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val type: TransactionType = TransactionType.WITHDRAWAL,
    val amount: String = "",
    val description: String = "",
    val date: Instant,
    val dateText: String = "",
    val sourceAccountId: String? = null,
    val sourceAccount: String = "",
    val destinationAccountId: String? = null,
    val destinationAccount: String = "",
    val category: String = "",
    val budget: String = "",
    val selectedTags: Set<String> = emptySet(),
    val notes: String = "",
    val currency: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val budgets: List<FireflyFeatureItem> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: TransactionEditorError? = null,
    val errorDetails: String? = null,
)
