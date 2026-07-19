/* Presentation-layer ViewModel for a complete cached transaction view. */
package com.teja.finfly.presentation.transactiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.presentation.navigation.AppRoute
import com.teja.finfly.domain.usecase.SyncFinancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Observes one Room row and separates its reference, readable notes, and optional raw SMS. */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
    private val syncFinances: SyncFinancesUseCase,
) : ViewModel() {
    private val transactionId = savedStateHandle.toRoute<AppRoute.TransactionDetail>().transactionId
    private val mutableDeletionState = kotlinx.coroutines.flow.MutableStateFlow(TransactionDeletionState())
    val deletionState = mutableDeletionState.asStateFlow()

    val uiState = repository.observeTransaction(transactionId).map { result ->
        when (result) {
            is Result.Error -> TransactionDetailUiState.Error
            is Result.Success -> result.value?.let { transaction ->
                val fields = extractFields(transaction.notes, transaction.rawSms)
                TransactionDetailUiState.Success(
                    transaction = transaction,
                    reference = fields.reference,
                    displayNotes = fields.notes,
                    rawSms = fields.rawSms,
                )
            } ?: TransactionDetailUiState.Empty
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailUiState.Loading)

    fun retry() {
        viewModelScope.launch { syncFinances() }
    }

    fun delete(transaction: com.teja.finfly.domain.model.Transaction) {
        if (mutableDeletionState.value.isDeleting) return
        viewModelScope.launch {
            mutableDeletionState.value = TransactionDeletionState(isDeleting = true)
            mutableDeletionState.value = when (repository.deleteTransaction(transaction.remoteGroupId)) {
                is Result.Success -> TransactionDeletionState(deleted = true)
                is Result.Error -> TransactionDeletionState(failed = true)
            }
        }
    }

    fun dismissDeleteError() {
        mutableDeletionState.value = TransactionDeletionState()
    }

    private fun extractFields(notes: String?, storedRawSms: String?): DetailFields {
        val source = notes.orEmpty()
        val rawMatch = RAW_SMS_PATTERN.find(source)
        val rawSms = storedRawSms?.takeIf(String::isNotBlank)
            ?: rawMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
        val withoutRaw = rawMatch?.let { source.removeRange(it.range) } ?: source
        val referenceMatch = REFERENCE_PATTERN.find(withoutRaw)
        val reference = referenceMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
        val cleanedNotes = referenceMatch?.let { withoutRaw.removeRange(it.range) } ?: withoutRaw
        return DetailFields(
            reference = reference,
            notes = cleanedNotes.trim().takeIf(String::isNotBlank),
            rawSms = rawSms,
        )
    }

    private data class DetailFields(val reference: String?, val notes: String?, val rawSms: String?)

    private companion object {
        val REFERENCE_PATTERN = Regex(
            "(?im)^\\s*(?:Ref(?:erence)?(?:\\s*(?:No\\.?|Number))?|UTR)\\s*[:#-]\\s*([^\\r\\n]+)\\s*$"
        )
        val RAW_SMS_PATTERN = Regex("(?is)(?:^|\\n)\\s*Raw\\s+SMS\\s*:\\s*(.+)$")
    }
}
