/* Presentation-layer ViewModel for basic Firefly bank-account creation. */
package com.teja.finfly.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AccountDraft
import com.teja.finfly.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

enum class AccountEditorError { NAME_REQUIRED, INVALID_BALANCE, SAVE_FAILED }

data class AccountEditorUiState(
    val name: String = "",
    val type: String = "asset",
    val currency: String = "",
    val openingBalance: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: AccountEditorError? = null,
)

/** Validates and submits one account, then lets the UI return to the account list. */
@HiltViewModel
class AccountEditorViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountEditorUiState())
    val uiState = _uiState.asStateFlow()

    fun setName(value: String) = update { copy(name = value, error = null) }
    fun setType(value: String) = update { copy(type = value, error = null) }
    fun setCurrency(value: String) = update { copy(currency = value.uppercase(), error = null) }
    fun setOpeningBalance(value: String) = update { copy(openingBalance = value, error = null) }

    fun save() {
        val state = _uiState.value
        val balance = state.openingBalance.takeIf(String::isNotBlank)?.toBigDecimalOrNull()
        val error = when {
            state.name.isBlank() -> AccountEditorError.NAME_REQUIRED
            state.openingBalance.isNotBlank() && balance == null -> AccountEditorError.INVALID_BALANCE
            else -> null
        }
        if (error != null) {
            update { copy(error = error) }
            return
        }
        viewModelScope.launch {
            update { copy(isSaving = true, error = null) }
            val result = repository.createAccount(
                AccountDraft(
                    name = state.name,
                    type = state.type,
                    currency = state.currency,
                    openingBalance = balance,
                    openingBalanceDate = balance?.let { clock.instant() },
                )
            )
            update {
                copy(
                    isSaving = false,
                    saved = result is Result.Success,
                    error = if (result is Result.Error) AccountEditorError.SAVE_FAILED else null,
                )
            }
        }
    }

    private fun update(transform: AccountEditorUiState.() -> AccountEditorUiState) {
        _uiState.value = _uiState.value.transform()
    }
}
