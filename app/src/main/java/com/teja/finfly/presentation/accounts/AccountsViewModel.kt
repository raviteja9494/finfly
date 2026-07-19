/* Presentation-layer ViewModel for grouped cached Firefly accounts. */
package com.teja.finfly.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.SyncState
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.usecase.SyncFinancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Combines every cached account type with pull-to-refresh feedback from the shared sync operation. */
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val syncFinances: SyncFinancesUseCase,
) : ViewModel() {
    private val mutableDeletionState = kotlinx.coroutines.flow.MutableStateFlow(AccountDeletionState())
    val deletionState = mutableDeletionState.asStateFlow()
    val uiState = combine(repository.observeAccounts(), syncFinances.state) { result, syncState ->
        val refreshing = syncState is SyncState.Syncing
        when (result) {
            is Result.Error -> AccountsUiState.Error
            is Result.Success -> if (result.value.isEmpty()) AccountsUiState.Empty(refreshing)
            else AccountsUiState.Success(result.value, refreshing)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState.Loading)

    fun refresh() {
        viewModelScope.launch { syncFinances() }
    }

    fun delete(accountId: String) {
        if (mutableDeletionState.value.deletingId != null) return
        viewModelScope.launch {
            mutableDeletionState.value = AccountDeletionState(deletingId = accountId)
            mutableDeletionState.value = when (repository.deleteAccount(accountId)) {
                is Result.Success -> AccountDeletionState()
                is Result.Error -> AccountDeletionState(failed = true)
            }
        }
    }

    fun dismissDeleteError() {
        mutableDeletionState.value = AccountDeletionState()
    }
}

/** Loading, grouped content, empty, and failure states for the Accounts screen. */
sealed interface AccountsUiState {
    data object Loading : AccountsUiState
    data class Success(val accounts: List<Account>, val isRefreshing: Boolean) : AccountsUiState
    data class Empty(val isRefreshing: Boolean) : AccountsUiState
    data object Error : AccountsUiState
}

data class AccountDeletionState(
    val deletingId: String? = null,
    val failed: Boolean = false,
)
