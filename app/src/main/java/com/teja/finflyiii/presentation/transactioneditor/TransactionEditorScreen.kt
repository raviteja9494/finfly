/* Presentation-layer Compose form for transaction groups and their splits. */
package com.teja.finflyiii.presentation.transactioneditor

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.teja.finflyiii.R
import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.TransactionType
import com.teja.finflyiii.presentation.components.DateTimePickerField
import com.teja.finflyiii.presentation.components.LoadingState
import com.teja.finflyiii.presentation.theme.FinFlyIIIThemeTokens

@Composable
fun TransactionEditorScreen(
    onBack: () -> Unit,
    viewModel: TransactionEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val saveFailure = state.errorDetails ?: stringResource(R.string.transaction_save_failed)
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
    val spacing = FinFlyIIIThemeTokens.spacing
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
        item {
            DateTimePickerField(
                value = state.dateText,
                onValueChange = viewModel::setDateText,
                label = R.string.date_and_time,
            )
        }
        item {
            OutlinedTextField(
                value = state.currency,
                onValueChange = viewModel::setCurrency,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.currency)) },
                supportingText = { Text(stringResource(R.string.iso_currency_example)) },
                singleLine = true,
            )
        }
        itemsIndexed(state.splits, key = { _, split -> split.key }) { index, split ->
            SplitCard(
                index = index,
                split = split,
                state = state,
                viewModel = viewModel,
                canRemove = state.splits.size > 1,
            )
        }
        item {
            OutlinedButton(onClick = viewModel::addSplit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.add_transaction_split), modifier = Modifier.padding(start = spacing.small))
            }
        }
        if (state.splits.size > 1) item {
            Text(
                stringResource(
                    R.string.transaction_split_total,
                    state.totalAmount.toPlainString(),
                    state.currency.ifBlank { stringResource(R.string.currency) },
                ),
                style = MaterialTheme.typography.titleMedium,
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
private fun SplitCard(
    index: Int,
    split: TransactionSplitUiState,
    state: TransactionEditorUiState,
    viewModel: TransactionEditorViewModel,
    canRemove: Boolean,
) {
    val spacing = FinFlyIIIThemeTokens.spacing
    var newTag by remember(split.key) { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.transaction_split_number, index + 1),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (canRemove) {
                    IconButton(onClick = { viewModel.removeSplit(index) }) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.remove_transaction_split),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = split.description,
                onValueChange = { viewModel.setDescription(index, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.description)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = split.amount,
                onValueChange = { viewModel.setAmount(index, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            AccountEntry(R.string.source_account, split.sourceAccount) { viewModel.setSourceName(index, it) }
            if (state.accounts.isNotEmpty()) {
                AccountChoices(state.accounts, split.sourceAccountId) { id, name ->
                    viewModel.selectSource(index, id, name)
                }
            }
            AccountEntry(R.string.destination_account, split.destinationAccount) {
                viewModel.setDestinationName(index, it)
            }
            if (state.accounts.isNotEmpty()) {
                AccountChoices(state.accounts, split.destinationAccountId) { id, name ->
                    viewModel.selectDestination(index, id, name)
                }
            }
            CategoryDropdown(state.categories, split.category) { viewModel.setCategory(index, it) }
            if (state.type == TransactionType.WITHDRAWAL) {
                SimpleDropdown(
                    label = R.string.budget,
                    choices = state.budgets.map { it.title },
                    selected = split.budget,
                    emptyLabel = R.string.no_budget,
                    onSelected = { viewModel.setBudget(index, it) },
                )
            }
            SectionLabel(R.string.tags)
            val tagChoices = (state.tags.map { it.name } + split.selectedTags).distinct()
            if (tagChoices.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    items(tagChoices, key = { it }) { tag ->
                        FilterChip(
                            selected = tag in split.selectedTags,
                            onClick = { viewModel.toggleTag(index, tag) },
                            label = { Text(tag) },
                        )
                    }
                }
            } else {
                Text(
                    stringResource(R.string.no_tags_available),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.add_tag)) },
                    singleLine = true,
                )
                OutlinedButton(onClick = { viewModel.addTag(index, newTag); newTag = "" }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_tag))
                }
            }
            OutlinedTextField(
                value = split.notes,
                onValueChange = { viewModel.setNotes(index, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.notes)) },
                minLines = 3,
            )
        }
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.small)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.xSmall)) {
        Text(stringResource(R.string.category), style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.ifBlank { stringResource(R.string.category_uncategorized) }, modifier = Modifier.weight(1f))
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
}

@Composable
private fun SimpleDropdown(
    label: Int,
    choices: List<String>,
    selected: String,
    emptyLabel: Int,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.xSmall)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.ifBlank { stringResource(emptyLabel) }, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(emptyLabel)) },
                    onClick = { onSelected(""); expanded = false },
                )
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice) },
                        onClick = { onSelected(choice); expanded = false },
                    )
                }
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
    TransactionEditorError.INVALID_CURRENCY -> R.string.invalid_currency_code
    TransactionEditorError.SAVE_FAILED -> R.string.transaction_save_failed
    TransactionEditorError.LOAD_FAILED -> R.string.transaction_load_failed
}
