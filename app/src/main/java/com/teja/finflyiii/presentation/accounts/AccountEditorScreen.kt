/* Presentation-layer Compose form for creating a Firefly bank account. */
package com.teja.finflyiii.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finflyiii.R
import com.teja.finflyiii.presentation.theme.FinFlyIIIThemeTokens
import com.teja.finflyiii.presentation.components.LoadingState

@Composable
fun AccountEditorScreen(
    onBack: () -> Unit,
    viewModel: AccountEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    if (state.isLoading) {
        LoadingState()
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(FinFlyIIIThemeTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.medium),
    ) {
        OutlinedButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = null)
            Text(stringResource(R.string.cancel), modifier = Modifier.padding(start = FinFlyIIIThemeTokens.spacing.small))
        }
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.account_name)) },
            singleLine = true,
        )
        Text(stringResource(R.string.account_type), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.small)) {
            (ACCOUNT_TYPES + state.type).distinct().forEach { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { viewModel.setType(type) },
                    label = { Text(accountTypeLabel(type)) },
                )
            }
        }
        OutlinedTextField(
            value = state.currency,
            onValueChange = viewModel::setCurrency,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.currency_optional)) },
            supportingText = { Text(stringResource(R.string.iso_currency_help)) },
            singleLine = true,
        )
        if (state.accountId == null) {
            OutlinedTextField(
                value = state.openingBalance,
                onValueChange = viewModel::setOpeningBalance,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.opening_balance_optional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        }
        state.error?.let {
            Text(stringResource(it.messageResource()), color = MaterialTheme.colorScheme.error)
            state.errorDetails?.let { details ->
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(when {
                state.isSaving -> R.string.saving
                state.accountId != null -> R.string.save_changes
                else -> R.string.create_bank_account
            }))
        }
    }
}

private fun AccountEditorError.messageResource(): Int = when (this) {
    AccountEditorError.NAME_REQUIRED -> R.string.account_name_required
    AccountEditorError.INVALID_BALANCE -> R.string.account_invalid_balance
    AccountEditorError.INVALID_CURRENCY -> R.string.invalid_currency_code
    AccountEditorError.SAVE_FAILED -> R.string.account_save_failed
}

private val ACCOUNT_TYPES = listOf("asset", "cash")

@Composable
private fun accountTypeLabel(type: String): String = when (type) {
    "asset" -> stringResource(R.string.account_asset)
    "cash" -> stringResource(R.string.account_cash)
    else -> type.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
