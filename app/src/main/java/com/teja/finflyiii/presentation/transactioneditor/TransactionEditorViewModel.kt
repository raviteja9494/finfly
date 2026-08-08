/* Presentation-layer ViewModel for Firefly transaction-group creation and editing. */
package com.teja.finflyiii.presentation.transactioneditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.common.isBlankOrIsoCurrencyCode
import com.teja.finflyiii.domain.model.FireflyFeature
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.TransactionDraft
import com.teja.finflyiii.domain.model.TransactionType
import com.teja.finflyiii.domain.repository.AccountRepository
import com.teja.finflyiii.domain.repository.FireflyFeatureRepository
import com.teja.finflyiii.domain.repository.TagRepository
import com.teja.finflyiii.domain.repository.TransactionRepository
import com.teja.finflyiii.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/** Loads selector metadata and persists one or more validated Firefly transaction splits. */
@HiltViewModel
class TransactionEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
    private val featureRepository: FireflyFeatureRepository,
    accountRepository: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AppRoute.TransactionEditor>()
    private val initialDate = clock.instant()
    private val splitKeys = AtomicLong(1)
    private val _uiState = MutableStateFlow(
        TransactionEditorUiState(
            date = initialDate,
            dateText = initialDate.toEditorText(),
            splits = listOf(emptySplit()),
        )
    )
    val uiState = _uiState.asStateFlow()
    private var initialized = false
    private var remoteGroupId: String? = null
    private val removedJournalIds = mutableSetOf<String>()

    init {
        val transactions: Flow<Result<List<Transaction>>> = route.transactionId
            ?.let(transactionRepository::observeTransactionGroup)
            ?: flowOf(Result.Success(emptyList()))
        viewModelScope.launch {
            combine(
                transactions,
                transactionRepository.observeCategories(),
                tagRepository.observeTags(),
                accountRepository.observeAccounts(),
                flow { emit(featureRepository.load(FireflyFeature.BUDGETS)) },
            ) { transactionResult, categoryResult, tagResult, accountResult, budgetResult ->
                EditorData(transactionResult, categoryResult, tagResult, accountResult, budgetResult)
            }.collect(::applyEditorData)
        }
    }

    fun setType(value: TransactionType) = update { copy(type = value, error = null) }
    fun setDateText(value: String) = update { copy(dateText = value, error = null) }
    fun setCurrency(value: String) = update { copy(currency = value.uppercase(), error = null) }
    fun setAmount(index: Int, value: String) = updateSplit(index) { copy(amount = value) }
    fun setDescription(index: Int, value: String) = updateSplit(index) { copy(description = value) }
    fun setSourceName(index: Int, value: String) = updateSplit(index) {
        copy(sourceAccount = value, sourceAccountId = null)
    }
    fun setDestinationName(index: Int, value: String) = updateSplit(index) {
        copy(destinationAccount = value, destinationAccountId = null)
    }
    fun selectSource(index: Int, id: String, name: String) = updateSplit(index) {
        copy(sourceAccountId = id, sourceAccount = name)
    }
    fun selectDestination(index: Int, id: String, name: String) = updateSplit(index) {
        copy(destinationAccountId = id, destinationAccount = name)
    }
    fun setCategory(index: Int, value: String) = updateSplit(index) { copy(category = value) }
    fun setBudget(index: Int, value: String) = updateSplit(index) { copy(budget = value) }
    fun setNotes(index: Int, value: String) = updateSplit(index) { copy(notes = value) }
    fun toggleTag(index: Int, value: String) = updateSplit(index) {
        copy(selectedTags = if (value in selectedTags) selectedTags - value else selectedTags + value)
    }
    fun addTag(index: Int, value: String) {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) updateSplit(index) { copy(selectedTags = selectedTags + trimmed) }
    }

    fun addSplit() = update {
        if (splits.size >= MAX_SPLITS) this else {
            val template = splits.firstOrNull()
            copy(
                splits = splits + emptySplit().copy(
                    sourceAccountId = template?.sourceAccountId,
                    sourceAccount = template?.sourceAccount.orEmpty(),
                    destinationAccountId = template?.destinationAccountId,
                    destinationAccount = template?.destinationAccount.orEmpty(),
                ),
                error = null,
            )
        }
    }

    fun removeSplit(index: Int) {
        val state = _uiState.value
        if (state.splits.size <= 1 || index !in state.splits.indices) return
        state.splits[index].journalId?.let(removedJournalIds::add)
        update { copy(splits = splits.filterIndexed { row, _ -> row != index }, error = null) }
    }

    fun clearError() = update { copy(error = null, errorDetails = null) }

    fun save() {
        val state = _uiState.value
        val parsedDate = runCatching {
            LocalDateTime.parse(state.dateText, EDITOR_DATE_FORMAT)
                .atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()
        val error = when {
            state.isEditing && remoteGroupId == null -> TransactionEditorError.LOAD_FAILED
            state.splits.any {
                it.description.isBlank() || it.sourceAccount.isBlank() || it.destinationAccount.isBlank()
            } -> TransactionEditorError.REQUIRED_FIELDS
            state.splits.any { (it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO } ->
                TransactionEditorError.INVALID_AMOUNT
            parsedDate == null -> TransactionEditorError.INVALID_DATE
            !state.currency.isBlankOrIsoCurrencyCode() -> TransactionEditorError.INVALID_CURRENCY
            else -> null
        }
        if (error != null) {
            update { copy(error = error) }
            return
        }
        val drafts = state.splits.map { split ->
            TransactionDraft(
                localId = split.localId,
                remoteGroupId = remoteGroupId,
                journalId = split.journalId,
                type = state.type,
                amount = split.amount.toBigDecimal(),
                description = split.description,
                date = parsedDate!!,
                sourceAccountId = split.sourceAccountId,
                sourceAccount = split.sourceAccount,
                destinationAccountId = split.destinationAccountId,
                destinationAccount = split.destinationAccount,
                category = split.category,
                budget = split.budget,
                tags = split.selectedTags.sorted(),
                notes = split.notes,
                currency = state.currency,
            )
        }
        viewModelScope.launch {
            update { copy(isSaving = true, error = null) }
            val result = transactionRepository.saveTransactionGroup(drafts, removedJournalIds.toSet())
            update {
                copy(
                    isSaving = false,
                    saved = result is Result.Success,
                    error = if (result is Result.Error) TransactionEditorError.SAVE_FAILED else null,
                    errorDetails = (result as? Result.Error)?.message,
                )
            }
        }
    }

    private fun applyEditorData(data: EditorData) {
        if (data.transactions is Result.Error) {
            update { copy(isLoading = false, error = TransactionEditorError.LOAD_FAILED) }
            return
        }
        val categories = data.categories.valuesOrEmpty()
        val tags = data.tags.valuesOrEmpty()
        val accounts = data.accounts.valuesOrEmpty()
        val budgets = data.budgets.valuesOrEmpty()
        val transactions = when (val result = data.transactions) {
            is Result.Success -> result.value
            is Result.Error -> emptyList()
        }
        if (!initialized) {
            initialized = true
            remoteGroupId = transactions.firstOrNull()?.remoteGroupId?.takeIf(String::isNotBlank)
            val first = transactions.firstOrNull()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isEditing = transactions.isNotEmpty(),
                type = first?.type ?: TransactionType.WITHDRAWAL,
                date = first?.date ?: initialDate,
                dateText = (first?.date ?: initialDate).toEditorText(),
                currency = first?.currency?.takeUnless { it == "XXX" }.orEmpty(),
                splits = transactions.map { it.toSplitState() }.ifEmpty { listOf(emptySplit()) },
                categories = categories,
                tags = tags,
                accounts = accounts,
                budgets = budgets,
            )
        } else {
            update { copy(categories = categories, tags = tags, accounts = accounts, budgets = budgets) }
        }
    }

    private fun Transaction.toSplitState() = TransactionSplitUiState(
        key = splitKeys.getAndIncrement(),
        localId = id,
        journalId = journalId.takeIf(String::isNotBlank),
        amount = amount.toPlainString(),
        description = description,
        sourceAccountId = sourceAccountId,
        sourceAccount = sourceAccount,
        destinationAccountId = destinationAccountId,
        destinationAccount = destinationAccount,
        category = category,
        budget = budget,
        selectedTags = tags.toSet(),
        notes = notes.orEmpty(),
    )

    private fun emptySplit() = TransactionSplitUiState(key = splitKeys.getAndIncrement())

    private fun updateSplit(index: Int, transform: TransactionSplitUiState.() -> TransactionSplitUiState) {
        update {
            if (index !in splits.indices) this else copy(
                splits = splits.toMutableList().also { it[index] = it[index].transform() },
                error = null,
            )
        }
    }

    private fun update(transform: TransactionEditorUiState.() -> TransactionEditorUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun <T> Result<List<T>>.valuesOrEmpty(): List<T> = when (this) {
        is Result.Success -> value
        is Result.Error -> emptyList()
    }

    private fun Instant.toEditorText(): String = atZone(ZoneId.systemDefault()).format(EDITOR_DATE_FORMAT)

    private data class EditorData(
        val transactions: Result<List<Transaction>>,
        val categories: Result<List<com.teja.finflyiii.domain.model.Category>>,
        val tags: Result<List<com.teja.finflyiii.domain.model.Tag>>,
        val accounts: Result<List<com.teja.finflyiii.domain.model.Account>>,
        val budgets: Result<List<com.teja.finflyiii.domain.model.FireflyFeatureItem>>,
    )

    private companion object {
        val EDITOR_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        const val MAX_SPLITS = 20
    }
}
