/* Presentation-layer ViewModel for cached Firefly accounts. */
package com.teja.finfly.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Maps the offline-first account stream into a compact screen state. */
@HiltViewModel
class AccountsViewModel @Inject constructor(repository: AccountRepository) : ViewModel() {
    val uiState = repository.observeAccounts().map { result ->
        when (result) {
            is Result.Success -> AccountsUiState(accounts = result.value.filter(Account::isBalanceAccount))
            is Result.Error -> AccountsUiState(isLoading = false, hasError = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())
}

data class AccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val hasError: Boolean = false,
)
