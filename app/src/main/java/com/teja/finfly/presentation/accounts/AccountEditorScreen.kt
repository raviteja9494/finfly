/* Presentation-layer Compose form for creating a Firefly bank account. */
package com.teja.finfly.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.presentation.theme.FinFlyThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorScreen(
    onBack: () -> Unit,
    viewModel: AccountEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_bank_account)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(FinFlyThemeTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.account_name)) },
                singleLine = true,
            )
            Text(stringResource(R.string.account_type), style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
            ) {
                ACCOUNT_TYPES.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(stringResource(if (type == "asset") R.string.account_asset else R.string.account_cash)) },
                    )
                }
            }
            OutlinedTextField(
                value = state.currency,
                onValueChange = viewModel::setCurrency,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.currency_optional)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.openingBalance,
                onValueChange = viewModel::setOpeningBalance,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.opening_balance_optional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            state.error?.let {
                Text(
                    stringResource(it.messageResource()),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (state.isSaving) R.string.saving else R.string.create_bank_account))
            }
        }
    }
}

private fun AccountEditorError.messageResource(): Int = when (this) {
    AccountEditorError.NAME_REQUIRED -> R.string.account_name_required
    AccountEditorError.INVALID_BALANCE -> R.string.account_invalid_balance
    AccountEditorError.SAVE_FAILED -> R.string.account_save_failed
}

private val ACCOUNT_TYPES = listOf("asset", "cash")
