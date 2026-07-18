/* Presentation-layer ViewModel for a complete cached transaction view. */
package com.teja.finfly.presentation.transactiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Observes one Room row and extracts a reference identifier from its notes. */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: TransactionRepository,
) : ViewModel() {
    private val transactionId = savedStateHandle.toRoute<AppRoute.TransactionDetail>().transactionId

    val uiState = repository.observeTransaction(transactionId).map { result ->
        when (result) {
            is Result.Error -> TransactionDetailUiState.Error
            is Result.Success -> result.value?.let { transaction ->
                TransactionDetailUiState.Success(
                    transaction = transaction,
                    reference = transaction.notes?.let(REFERENCE_PATTERN::find)?.groupValues?.getOrNull(1)?.trim(),
                )
            } ?: TransactionDetailUiState.Empty
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailUiState.Loading)

    private companion object {
        val REFERENCE_PATTERN = Regex("(?im)^\\s*Ref(?:erence)?\\s*:\\s*(.+?)\\s*$")
    }
}
