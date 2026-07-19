/* Presentation-layer ViewModel for basic Firefly bank-account creation. */
package com.teja.finflyiii.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.teja.finflyiii.presentation.navigation.AppRoute
import androidx.lifecycle.viewModelScope
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.common.isBlankOrIsoCurrencyCode
import com.teja.finflyiii.domain.model.AccountDraft
import com.teja.finflyiii.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

enum class AccountEditorError { NAME_REQUIRED, INVALID_BALANCE, INVALID_CURRENCY, SAVE_FAILED }

data class AccountEditorUiState(
    val accountId: String? = null,
    val isLoading: Boolean = false,
    val name: String = "",
    val type: String = "asset",
    val currency: String = "",
    val openingBalance: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: AccountEditorError? = null,
    val errorDetails: String? = null,
)

/** Validates and submits one account, then lets the UI return to the account list. */
@HiltViewModel
class AccountEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AccountRepository,
    private val clock: Clock,
) : ViewModel() {
    private val accountId = savedStateHandle.toRoute<AppRoute.AccountEditor>().accountId
    private val _uiState = MutableStateFlow(AccountEditorUiState(accountId = accountId, isLoading = accountId != null))
    val uiState = _uiState.asStateFlow()

    init {
        accountId?.let { id ->
            viewModelScope.launch {
                repository.observeAccount(id).collect { result ->
                    update {
                        when (result) {
                            is Result.Success -> result.value?.let { account ->
                                copy(
                                    isLoading = false,
                                    name = account.name,
                                    type = account.type.lowercase(),
                                    currency = account.currency.takeUnless { it == "XXX" }.orEmpty(),
                                )
                            } ?: copy(isLoading = false, error = AccountEditorError.SAVE_FAILED)
                            is Result.Error -> copy(isLoading = false, error = AccountEditorError.SAVE_FAILED)
                        }
                    }
                }
            }
        }
    }

    fun setName(value: String) = update { copy(name = value, error = null) }
    fun setType(value: String) = update { copy(type = value, error = null) }
    fun setCurrency(value: String) = update { copy(currency = value.uppercase(), error = null) }
    fun setOpeningBalance(value: String) = update { copy(openingBalance = value, error = null) }

    fun save() {
        val state = _uiState.value
        val balance = state.openingBalance.takeIf(String::isNotBlank)?.toBigDecimalOrNull()
        val error = when {
            state.name.isBlank() -> AccountEditorError.NAME_REQUIRED
            !state.currency.isBlankOrIsoCurrencyCode() -> AccountEditorError.INVALID_CURRENCY
            state.openingBalance.isNotBlank() && balance == null -> AccountEditorError.INVALID_BALANCE
            else -> null
        }
        if (error != null) {
            update { copy(error = error) }
            return
        }
        viewModelScope.launch {
            update { copy(isSaving = true, error = null) }
            val result = repository.saveAccount(
                AccountDraft(
                    id = accountId,
                    name = state.name,
                    type = state.type,
                    currency = state.currency,
                    openingBalance = balance,
                    openingBalanceDate = if (accountId == null) balance?.let { clock.instant() } else null,
                )
            )
            update {
                copy(
                    isSaving = false,
                    saved = result is Result.Success,
                    error = if (result is Result.Error) AccountEditorError.SAVE_FAILED else null,
                    errorDetails = (result as? Result.Error)?.message,
                )
            }
        }
    }

    private fun update(transform: AccountEditorUiState.() -> AccountEditorUiState) {
        _uiState.value = _uiState.value.transform()
    }
}
