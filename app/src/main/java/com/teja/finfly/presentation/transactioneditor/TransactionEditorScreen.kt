/* Presentation-layer Compose form shared by transaction creation and editing. */
package com.teja.finfly.presentation.transactioneditor

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorScreen(
    onBack: () -> Unit,
    viewModel: TransactionEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isEditing) R.string.edit_transaction else R.string.new_transaction))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) LoadingState(Modifier.padding(padding))
        else TransactionForm(state, viewModel, padding)
    }
}

@Composable
private fun TransactionForm(
    state: TransactionEditorUiState,
    viewModel: TransactionEditorViewModel,
    contentPadding: PaddingValues,
) {
    val spacing = FinFlyThemeTokens.spacing
    var newTag by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.medium,
            end = spacing.medium,
            top = contentPadding.calculateTopPadding() + spacing.small,
            bottom = contentPadding.calculateBottomPadding() + spacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
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
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.description)) },
                singleLine = true,
            )
        }
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
        item {
            Text(
                stringResource(
                    R.string.transaction_date_value,
                    state.date.atZone(ZoneId.systemDefault()).format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    ),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item { SectionLabel(R.string.source_account) }
        item {
            OutlinedTextField(
                value = state.sourceAccount,
                onValueChange = viewModel::setSourceName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.source_account_hint)) },
                singleLine = true,
            )
        }
        if (state.accounts.isNotEmpty()) item {
            AccountChoices(
                accounts = state.accounts,
                selectedId = state.sourceAccountId,
                onSelect = viewModel::selectSource,
            )
        }
        item { SectionLabel(R.string.destination_account) }
        item {
            OutlinedTextField(
                value = state.destinationAccount,
                onValueChange = viewModel::setDestinationName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.destination_account_hint)) },
                singleLine = true,
            )
        }
        if (state.accounts.isNotEmpty()) item {
            AccountChoices(
                accounts = state.accounts,
                selectedId = state.destinationAccountId,
                onSelect = viewModel::selectDestination,
            )
        }
        item { SectionLabel(R.string.categories) }
        item {
            OutlinedTextField(
                value = state.category,
                onValueChange = viewModel::setCategory,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.category_name)) },
                singleLine = true,
            )
        }
        if (state.categories.isNotEmpty()) item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = state.category == category.name,
                        onClick = { viewModel.setCategory(category.name) },
                        label = { Text(category.name) },
                    )
                }
            }
        }
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
                OutlinedButton(
                    onClick = {
                        viewModel.addTag(newTag)
                        newTag = ""
                    },
                ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_tag)) }
            }
        }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.notes)) },
                minLines = 3,
            )
        }
        state.error?.let { error ->
            item {
                Text(
                    stringResource(error.messageResource()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
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

@Composable
private fun AccountChoices(
    accounts: List<com.teja.finfly.domain.model.Account>,
    selectedId: String?,
    onSelect: (String, String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small)) {
        items(accounts, key = { it.id }) { account ->
            FilterChip(
                selected = selectedId == account.id,
                onClick = { onSelect(account.id, account.name) },
                label = { Text(account.name) },
            )
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
    TransactionEditorError.SAVE_FAILED -> R.string.transaction_save_failed
    TransactionEditorError.LOAD_FAILED -> R.string.transaction_load_failed
}
