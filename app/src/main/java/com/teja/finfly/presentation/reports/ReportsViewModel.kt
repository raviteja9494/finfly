/* Presentation-layer ViewModel for the compact cached Reports screen. */
package com.teja.finfly.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.SyncState
import com.teja.finfly.domain.usecase.ObserveReportsUseCase
import com.teja.finfly.domain.usecase.SyncFinancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Combines cached report aggregates with the application-wide synchronization state. */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    observeReports: ObserveReportsUseCase,
    private val syncFinances: SyncFinancesUseCase,
) : ViewModel() {
    val uiState = combine(observeReports(), syncFinances.state) { result, syncState ->
        when (result) {
            is Result.Error -> ReportsUiState.Error
            is Result.Success -> if (result.value.transactionCount == 0) {
                ReportsUiState.Empty(syncState is SyncState.Syncing)
            } else {
                ReportsUiState.Success(result.value, syncState is SyncState.Syncing)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState.Loading)

    fun refresh() {
        viewModelScope.launch { syncFinances() }
    }
}
