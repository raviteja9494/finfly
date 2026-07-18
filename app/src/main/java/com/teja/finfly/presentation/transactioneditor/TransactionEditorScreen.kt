/* Presentation-layer Compose forms for transaction creation and constrained editing. */
package com.teja.finfly.presentation.transactioneditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.Category
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.theme.FinFlyThemeTokens

@Composable
fun TransactionEditorScreen(
    onBack: () -> Unit,
    viewModel: TransactionEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val saveFailure = stringResource(R.string.transaction_save_failed)
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    LaunchedEffect(state.error) {
        if (state.error == TransactionEditorError.SAVE_FAILED) {
            snackbar.showSnackbar(saveFailure)
            viewModel.clearError()
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (state.isLoading) LoadingState(Modifier.padding(padding))
        else EditorForm(state, viewModel, onBack, padding)
    }
}

@Composable
private fun EditorForm(
    state: TransactionEditorUiState,
    viewModel: TransactionEditorViewModel,
    onCancel: () -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = FinFlyThemeTokens.spacing
    var newTag by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.medium,
            end = spacing.medium,
            top = contentPadding.calculateTopPadding() + spacing.medium,
            bottom = contentPadding.calculateBottomPadding() + spacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onCancel) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    Text(stringResource(R.string.cancel), modifier = Modifier.padding(start = spacing.small))
                }
                Text(
                    stringResource(if (state.isEditing) R.string.edit_transaction else R.string.new_transaction),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        if (state.isEditing) {
            item {
                ReadOnlyTransactionFields(state)
            }
        } else {
            item { SectionLabel(R.string.transaction_type) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    items(TransactionType.entries) { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { viewModel.setType(type) },
                            label = { Text(type.label()) },
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.description)) },
                singleLine = true,
            )
        }
        if (!state.isEditing) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = viewModel::setAmount,
                        modifier = Modifier.weight(2f),
                        label = { Text(stringResource(R.string.amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.currency,
                        onValueChange = viewModel::setCurrency,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.currency)) },
                        singleLine = true,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.dateText,
                onValueChange = viewModel::setDateText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.date_and_time)) },
                supportingText = { Text(stringResource(R.string.date_time_hint)) },
                singleLine = true,
            )
        }
        if (!state.isEditing) {
            item { AccountEntry(R.string.source_account, state.sourceAccount, viewModel::setSourceName) }
            if (state.accounts.isNotEmpty()) item {
                AccountChoices(state.accounts, state.sourceAccountId, viewModel::selectSource)
            }
            item { AccountEntry(R.string.destination_account, state.destinationAccount, viewModel::setDestinationName) }
            if (state.accounts.isNotEmpty()) item {
                AccountChoices(state.accounts, state.destinationAccountId, viewModel::selectDestination)
            }
        }
        item {
            CategoryDropdown(
                categories = state.categories,
                selected = state.category,
                onSelected = viewModel::setCategory,
            )
        }
        if (!state.isEditing) {
            item { SectionLabel(R.string.tags) }
            if (state.tags.isNotEmpty() || state.selectedTags.isNotEmpty()) item {
                val choices = (state.tags.map { it.name } + state.selectedTags).distinct()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    items(choices, key = { it }) { tag ->
                        FilterChip(
                            selected = tag in state.selectedTags,
                            onClick = { viewModel.toggleTag(tag) },
                            label = { Text(tag) },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.add_tag)) },
                        singleLine = true,
                    )
                    OutlinedButton(onClick = { viewModel.addTag(newTag); newTag = "" }) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_tag))
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.notes)) },
                minLines = 4,
            )
        }
        state.error?.takeUnless { it == TransactionEditorError.SAVE_FAILED }?.let { error ->
            item { Text(stringResource(error.messageResource()), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !state.isSaving) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = viewModel::save, modifier = Modifier.weight(1f), enabled = !state.isSaving) {
                    Text(
                        stringResource(
                            when {
                                state.isSaving -> R.string.saving
                                state.isEditing -> R.string.save_changes
                                else -> R.string.create_transaction
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyTransactionFields(state: TransactionEditorUiState) {
    val spacing = FinFlyThemeTokens.spacing
    val accountPath = stringResource(
        R.string.account_path_format,
        state.sourceAccount,
        state.destinationAccount,
    )
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        OutlinedTextField(
            value = state.type.label(), onValueChange = {}, enabled = false,
            modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.transaction_type)) },
        )
        OutlinedTextField(
            value = listOf(state.amount, state.currency).filter(String::isNotBlank).joinToString(" "),
            onValueChange = {}, enabled = false, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.amount)) },
        )
        OutlinedTextField(
            value = accountPath,
            onValueChange = {}, enabled = false, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.account)) },
        )
    }
}

@Composable
private fun AccountEntry(label: Int, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        singleLine = true,
    )
}

@Composable
private fun AccountChoices(accounts: List<Account>, selectedId: String?, onSelect: (String, String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small)) {
        items(accounts, key = Account::id) { account ->
            FilterChip(
                selected = selectedId == account.id,
                onClick = { onSelect(account.id, account.name) },
                label = { Text(account.name) },
            )
        }
    }
}

@Composable
private fun CategoryDropdown(categories: List<Category>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                selected.ifBlank { stringResource(R.string.category_uncategorized) },
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.category_uncategorized)) },
                onClick = { expanded = false; onSelected("") },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = { expanded = false; onSelected(category.name) },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(resource: Int) {
    Text(stringResource(resource), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun TransactionType.label(): String = stringResource(
    when (this) {
        TransactionType.WITHDRAWAL -> R.string.transaction_withdrawal
        TransactionType.DEPOSIT -> R.string.transaction_deposit
        TransactionType.TRANSFER -> R.string.transaction_transfer
    }
)

private fun TransactionEditorError.messageResource(): Int = when (this) {
    TransactionEditorError.REQUIRED_FIELDS -> R.string.transaction_required_fields
    TransactionEditorError.INVALID_AMOUNT -> R.string.transaction_invalid_amount
    TransactionEditorError.INVALID_DATE -> R.string.transaction_invalid_date
    TransactionEditorError.SAVE_FAILED -> R.string.transaction_save_failed
    TransactionEditorError.LOAD_FAILED -> R.string.transaction_load_failed
}
