/* Presentation-layer Compose editors for friendly bank and category rules. */
package com.teja.finflyiii.presentation.smsrules

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.InputChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finflyiii.R
import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.SmsParseResult
import com.teja.finflyiii.presentation.theme.FinFlyIIIThemeTokens

@Composable
fun BankRuleEditorScreen(
    onBack: () -> Unit,
    viewModel: BankRuleEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onBack() }
    val spacing = FinFlyIIIThemeTokens.spacing
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item { BackButton(onBack) }
        item {
            OutlinedTextField(
                state.name,
                viewModel::setName,
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rule_name)) },
                singleLine = true,
            )
        }
        item { ToggleRow(R.string.rule_enabled, state.enabled, viewModel::setEnabled) }
        item { AccountDropdown(state.accounts, state.accountId, viewModel::setAccount) }
        item { ChipInput(R.string.sender_ids, R.string.sender_ids_hint, state.senderIds, viewModel::addSender, viewModel::removeSender) }
        item { ChipInput(R.string.debit_keywords, R.string.debit_keywords_hint, state.debitKeywords, viewModel::addDebit, viewModel::removeDebit) }
        item { ChipInput(R.string.credit_keywords, R.string.credit_keywords_hint, state.creditKeywords, viewModel::addCredit, viewModel::removeCredit) }
        item { ChipInput(R.string.amount_patterns, R.string.amount_pattern_hint, state.amountPatterns, viewModel::addAmount, viewModel::removeAmount) }
        item { ChipInput(R.string.description_patterns, R.string.description_pattern_hint, state.descriptionPatterns, viewModel::addDescription, viewModel::removeDescription) }
        item { ChipInput(R.string.reference_patterns, R.string.reference_pattern_hint, state.referencePatterns, viewModel::addReference, viewModel::removeReference) }
        item { ChipInput(R.string.firefly_tags, R.string.firefly_tags_hint, state.fireflyTags, viewModel::addTag, viewModel::removeTag) }
        item {
            Text(stringResource(R.string.test_this_rule), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                state.sampleSms,
                viewModel::setSample,
                Modifier.fillMaxWidth().padding(top = spacing.small),
                label = { Text(stringResource(R.string.sample_sms)) },
                minLines = 4,
            )
            OutlinedButton(
                onClick = viewModel::test,
                enabled = state.sampleSms.isNotBlank(),
                modifier = Modifier.padding(top = spacing.small),
            ) { Text(stringResource(R.string.test_rule)) }
        }
        state.testResult?.let { result -> item { TestResultCard(result) } }
        state.error?.let { error -> item { ErrorText(error.messageResource()) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                if (state.existing) OutlinedButton(onClick = viewModel::delete, Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete_rule))
                }
                Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.isSaving) R.string.saving else R.string.save_rule))
                }
            }
        }
    }
}

@Composable
fun CategoryRuleEditorScreen(
    onBack: () -> Unit,
    viewModel: CategoryRuleEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onBack() }
    val spacing = FinFlyIIIThemeTokens.spacing
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item { BackButton(onBack) }
        item {
            OutlinedTextField(
                state.name, viewModel::setName, Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rule_name)) }, singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                state.fireflyCategory, viewModel::setCategory, Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.firefly_category_name_optional)) }, singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                state.priority, viewModel::setPriority, Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rule_priority)) },
                supportingText = { Text(stringResource(R.string.rule_priority_description)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
            )
        }
        item { ToggleRow(R.string.rule_enabled, state.enabled, viewModel::setEnabled) }
        item { ChipInput(R.string.category_keywords, R.string.category_keywords_hint, state.keywords, viewModel::addKeyword, viewModel::removeKeyword) }
        item { ChipInput(R.string.firefly_tags, R.string.firefly_tags_hint, state.fireflyTags, viewModel::addTag, viewModel::removeTag) }
        state.error?.let { error -> item { ErrorText(error.messageResource()) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                if (state.existing) OutlinedButton(onClick = viewModel::delete, Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete_rule))
                }
                Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.isSaving) R.string.saving else R.string.save_rule))
                }
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) {
        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
        Text(stringResource(R.string.cancel), Modifier.padding(start = FinFlyIIIThemeTokens.spacing.small))
    }
}

@Composable
private fun ToggleRow(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(accounts: List<Account>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            selected?.name.orEmpty(), {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
            label = { Text(stringResource(R.string.firefly_account)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = { onSelect(account.id); expanded = false },
                )
            }
        }
    }
}

@Composable
internal fun ChipInput(
    label: Int,
    hint: Int,
    values: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var input by rememberSaveable(label) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.small)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        if (values.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.small)) {
                items(values, key = { it }) { value ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(value) },
                        label = { Text(value) },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.remove_item)) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { value ->
                if (',' in value) {
                    onAdd(value.substringBeforeLast(','))
                    input = value.substringAfterLast(',')
                } else input = value
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(hint)) },
            supportingText = { Text(stringResource(R.string.chip_input_help)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd(input); input = "" }),
            singleLine = true,
        )
    }
}

@Composable
private fun TestResultCard(result: SmsParseResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(FinFlyIIIThemeTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.xSmall),
        ) {
            when (result) {
                is SmsParseResult.Success -> {
                    val transaction = result.transaction
                    Text(stringResource(R.string.rule_test_matched), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.rule_test_amount, transaction.amount))
                    Text(stringResource(R.string.rule_test_type, transaction.type.name))
                    Text(stringResource(R.string.rule_test_description, transaction.description))
                    Text(stringResource(R.string.rule_test_reference, transaction.reference))
                    Text(stringResource(R.string.rule_test_category, transaction.category))
                }
                is SmsParseResult.Skipped -> Text(stringResource(result.reason.reasonResource()))
                is SmsParseResult.NoRuleMatched -> Text(stringResource(R.string.no_rule_matched))
            }
        }
    }
}

@Composable
private fun ErrorText(message: Int) {
    Text(stringResource(message), color = MaterialTheme.colorScheme.error)
}

private fun BankRuleEditorError.messageResource(): Int = when (this) {
    BankRuleEditorError.NAME -> R.string.rule_name_required
    BankRuleEditorError.ACCOUNT -> R.string.rule_account_required
    BankRuleEditorError.SENDER -> R.string.rule_sender_required
    BankRuleEditorError.KEYWORDS -> R.string.rule_keywords_required
    BankRuleEditorError.AMOUNT_PATTERN -> R.string.rule_amount_pattern_required
    BankRuleEditorError.DESCRIPTION_PATTERN -> R.string.rule_description_pattern_required
    BankRuleEditorError.SAVE -> R.string.rule_save_failed
}

private fun CategoryRuleEditorError.messageResource(): Int = when (this) {
    CategoryRuleEditorError.NAME -> R.string.rule_name_required
    CategoryRuleEditorError.TARGET -> R.string.rule_category_or_tag_required
    CategoryRuleEditorError.PRIORITY -> R.string.rule_priority_invalid
    CategoryRuleEditorError.KEYWORDS -> R.string.rule_keywords_required
    CategoryRuleEditorError.SAVE -> R.string.rule_save_failed
}

private fun String.reasonResource(): Int = when (this) {
    "type_not_found" -> R.string.parse_type_not_found
    "amount_not_found" -> R.string.parse_amount_not_found
    "description_not_found" -> R.string.parse_description_not_found
    else -> R.string.rule_test_failed
}
