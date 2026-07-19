/* Presentation ViewModels for SMS processing history. */
package com.teja.finflyiii.presentation.smsrules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.SmsLog
import com.teja.finflyiii.domain.repository.SmsLogRepository
import com.teja.finflyiii.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SmsLogsUiState {
    data object Loading : SmsLogsUiState
    data object Error : SmsLogsUiState
    data object Empty : SmsLogsUiState
    data class Success(val logs: List<SmsLog>) : SmsLogsUiState
}

/** Observes the capped newest-first SMS processing history. */
@HiltViewModel
class SmsLogsViewModel @Inject constructor(repository: SmsLogRepository) : ViewModel() {
    val uiState = repository.observeLogs().map { result ->
        when (result) {
            is Result.Error -> SmsLogsUiState.Error
            is Result.Success -> if (result.value.isEmpty()) SmsLogsUiState.Empty else SmsLogsUiState.Success(result.value)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmsLogsUiState.Loading)
}

sealed interface SmsLogDetailUiState {
    data object Loading : SmsLogDetailUiState
    data object Error : SmsLogDetailUiState
    data object Empty : SmsLogDetailUiState
    data class Success(val log: SmsLog) : SmsLogDetailUiState
}

/** Observes one SMS log selected from the diagnostics list. */
@HiltViewModel
class SmsLogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: SmsLogRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<AppRoute.SmsLogDetail>().id
    val uiState = repository.observeLog(id).map { result ->
        when (result) {
            is Result.Error -> SmsLogDetailUiState.Error
            is Result.Success -> result.value?.let(SmsLogDetailUiState::Success) ?: SmsLogDetailUiState.Empty
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmsLogDetailUiState.Loading)
}
