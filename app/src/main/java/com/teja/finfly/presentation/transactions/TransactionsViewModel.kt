/* Presentation-layer ViewModel managing the Room-backed paginated transaction list. */
package com.teja.finfly.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.usecase.ObserveTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Manages the growing Room page size and maps repository outcomes to screen-specific states. */
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    observeTransactions: ObserveTransactionsUseCase,
) : ViewModel() {
    private val requestedCount = MutableStateFlow(PAGE_SIZE)

    val uiState = requestedCount.flatMapLatest { count ->
        observeTransactions(count, 0).map { result ->
            when (result) {
                is Result.Error -> TransactionsUiState.Error
                is Result.Success -> when {
                    result.value.isEmpty() -> TransactionsUiState.Empty
                    else -> TransactionsUiState.Success(
                        transactions = result.value,
                        isLoadingMore = result.value.size >= count,
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState.Loading)

    fun loadMore() {
        val state = uiState.value
        if (state is TransactionsUiState.Success && state.isLoadingMore) {
            requestedCount.value += PAGE_SIZE
        }
    }

    private companion object { const val PAGE_SIZE = 25 }
}
