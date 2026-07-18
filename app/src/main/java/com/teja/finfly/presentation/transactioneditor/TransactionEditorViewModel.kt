/* Presentation-layer ViewModel for Firefly transaction creation and editing. */
package com.teja.finfly.presentation.transactioneditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.TransactionDraft
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Loads selector metadata and persists a validated create/update request. */
@HiltViewModel
class TransactionEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AppRoute.TransactionEditor>()
    private val initialDate = clock.instant()
    private val _uiState = MutableStateFlow(
        TransactionEditorUiState(date = initialDate, dateText = initialDate.toEditorText())
    )
    val uiState = _uiState.asStateFlow()
    private var initialized = false
    private var remoteGroupId: String? = null
    private var journalId: String? = null

    init {
        val transaction: Flow<Result<com.teja.finfly.domain.model.Transaction?>> =
            route.transactionId?.let(transactionRepository::observeTransaction)
            ?: flowOf(Result.Success(null))
        viewModelScope.launch {
            combine(
                transaction,
                transactionRepository.observeCategories(),
                transactionRepository.observeTags(),
                accountRepository.observeAccounts(),
            ) { transactionResult, categoryResult, tagResult, accountResult ->
                EditorData(transactionResult, categoryResult, tagResult, accountResult)
            }.collect(::applyEditorData)
        }
    }

    fun setType(value: TransactionType) = update { copy(type = value, error = null) }
    fun setAmount(value: String) = update { copy(amount = value, error = null) }
    fun setDescription(value: String) = update { copy(description = value, error = null) }
    fun setDateText(value: String) = update { copy(dateText = value, error = null) }
    fun setSourceName(value: String) = update {
        copy(sourceAccount = value, sourceAccountId = null, error = null)
    }
    fun setDestinationName(value: String) = update {
        copy(destinationAccount = value, destinationAccountId = null, error = null)
    }
    fun selectSource(id: String, name: String) = update {
        copy(sourceAccountId = id, sourceAccount = name, error = null)
    }
    fun selectDestination(id: String, name: String) = update {
        copy(destinationAccountId = id, destinationAccount = name, error = null)
    }
    fun setCategory(value: String) = update { copy(category = value, error = null) }
    fun setNotes(value: String) = update { copy(notes = value, error = null) }
    fun setCurrency(value: String) = update { copy(currency = value.uppercase(), error = null) }
    fun toggleTag(value: String) = update {
        copy(selectedTags = if (value in selectedTags) selectedTags - value else selectedTags + value)
    }
    fun addTag(value: String) {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) update { copy(selectedTags = selectedTags + trimmed) }
    }
    fun clearError() = update { copy(error = null) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toBigDecimalOrNull()
        val parsedDate = runCatching {
            LocalDateTime.parse(state.dateText, EDITOR_DATE_FORMAT)
                .atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()
        val error = when {
            state.isEditing && (remoteGroupId == null || journalId == null) ->
                TransactionEditorError.LOAD_FAILED
            state.description.isBlank() || state.sourceAccount.isBlank() || state.destinationAccount.isBlank() ->
                TransactionEditorError.REQUIRED_FIELDS
            amount == null || amount <= BigDecimal.ZERO -> TransactionEditorError.INVALID_AMOUNT
            parsedDate == null -> TransactionEditorError.INVALID_DATE
            else -> null
        }
        if (error != null) {
            update { copy(error = error) }
            return
        }
        viewModelScope.launch {
            update { copy(isSaving = true, error = null) }
            val result = transactionRepository.saveTransaction(
                TransactionDraft(
                    localId = route.transactionId,
                    remoteGroupId = remoteGroupId,
                    journalId = journalId,
                    type = state.type,
                    amount = amount!!,
                    description = state.description,
                    date = parsedDate!!,
                    sourceAccountId = state.sourceAccountId,
                    sourceAccount = state.sourceAccount,
                    destinationAccountId = state.destinationAccountId,
                    destinationAccount = state.destinationAccount,
                    category = state.category,
                    tags = state.selectedTags.sorted(),
                    notes = state.notes,
                    currency = state.currency,
                )
            )
            update {
                copy(
                    isSaving = false,
                    saved = result is Result.Success,
                    error = if (result is Result.Error) TransactionEditorError.SAVE_FAILED else null,
                )
            }
        }
    }

    private fun applyEditorData(data: EditorData) {
        if (data.transaction is Result.Error) {
            update { copy(isLoading = false, error = TransactionEditorError.LOAD_FAILED) }
            return
        }
        val categories = data.categories.valuesOrEmpty()
        val tags = data.tags.valuesOrEmpty()
        val accounts = data.accounts.valuesOrEmpty()
        val transaction = when (val result = data.transaction) {
            is Result.Success -> result.value
            is Result.Error -> null
        }
        if (!initialized) {
            initialized = true
            remoteGroupId = transaction?.remoteGroupId?.takeIf(String::isNotBlank)
            journalId = transaction?.journalId?.takeIf(String::isNotBlank)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isEditing = transaction != null,
                type = transaction?.type ?: TransactionType.WITHDRAWAL,
                amount = transaction?.amount?.toPlainString().orEmpty(),
                description = transaction?.description.orEmpty(),
                date = transaction?.date ?: initialDate,
                dateText = (transaction?.date ?: initialDate).toEditorText(),
                sourceAccountId = transaction?.sourceAccountId,
                sourceAccount = transaction?.sourceAccount.orEmpty(),
                destinationAccountId = transaction?.destinationAccountId,
                destinationAccount = transaction?.destinationAccount.orEmpty(),
                category = transaction?.category.orEmpty(),
                selectedTags = transaction?.tags?.toSet().orEmpty(),
                notes = transaction?.notes.orEmpty(),
                currency = transaction?.currency?.takeUnless { it == "XXX" }.orEmpty(),
                categories = categories,
                tags = tags,
                accounts = accounts,
            )
        } else {
            update { copy(categories = categories, tags = tags, accounts = accounts) }
        }
    }

    private fun update(transform: TransactionEditorUiState.() -> TransactionEditorUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun <T> Result<List<T>>.valuesOrEmpty(): List<T> = when (this) {
        is Result.Success -> value
        is Result.Error -> emptyList()
    }

    private fun java.time.Instant.toEditorText(): String = atZone(ZoneId.systemDefault())
        .format(EDITOR_DATE_FORMAT)

    private data class EditorData(
        val transaction: Result<com.teja.finfly.domain.model.Transaction?>,
        val categories: Result<List<com.teja.finfly.domain.model.Category>>,
        val tags: Result<List<com.teja.finfly.domain.model.Tag>>,
        val accounts: Result<List<com.teja.finfly.domain.model.Account>>,
    )

    private companion object {
        val EDITOR_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
