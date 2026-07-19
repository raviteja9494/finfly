/* Presentation-layer Compose forms for secondary Firefly resources. */
package com.teja.finflyiii.presentation.featureeditor

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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.IconButton
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
import com.teja.finflyiii.domain.model.FireflyFeature
import com.teja.finflyiii.domain.model.FireflyRuleClause
import com.teja.finflyiii.presentation.components.DatePickerField
import com.teja.finflyiii.presentation.components.LoadingState
import com.teja.finflyiii.presentation.theme.FinFlyIIIThemeTokens

@Composable
fun FeatureEditorScreen(
    onBack: () -> Unit,
    viewModel: FeatureEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    if (state.isLoading) {
        LoadingState()
        return
    }
    val spacing = FinFlyIIIThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            OutlinedButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.cancel), Modifier.padding(start = spacing.small))
            }
        }
        item {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(state.feature.nameLabel())) },
                singleLine = true,
            )
        }
        when (state.feature) {
            FireflyFeature.BUDGETS -> {
                item {
                    DecimalField(
                        value = state.minimumAmount,
                        onValueChange = viewModel::setMinimumAmount,
                        label = R.string.monthly_budget_amount_optional,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.currencyCode,
                        onValueChange = viewModel::setCurrencyCode,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.currency_optional)) },
                        singleLine = true,
                    )
                }
            }
            FireflyFeature.CATEGORIES -> Unit
            FireflyFeature.TAGS -> Unit
            FireflyFeature.BILLS -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        DecimalField(
                            state.minimumAmount,
                            viewModel::setMinimumAmount,
                            R.string.minimum_amount,
                            Modifier.weight(1f),
                        )
                        DecimalField(
                            state.maximumAmount,
                            viewModel::setMaximumAmount,
                            R.string.maximum_amount,
                            Modifier.weight(1f),
                        )
                    }
                }
                item { CurrencyField(state.currencyCode, viewModel::setCurrencyCode) }
                item { DateField(state.startDate, viewModel::setStartDate, R.string.first_due_date) }
                item {
                    Text(stringResource(R.string.repeat_frequency), style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        items(REPEAT_FREQUENCIES) { frequency ->
                            FilterChip(
                                selected = state.repeatFrequency == frequency,
                                onClick = { viewModel.setRepeatFrequency(frequency) },
                                label = { Text(stringResource(frequency.labelResource())) },
                            )
                        }
                    }
                }
            }
            FireflyFeature.PIGGY_BANKS -> {
                item {
                    AccountSelector(
                        state = state,
                        onSelect = viewModel::setAccountId,
                    )
                }
                item {
                    DecimalField(
                        state.minimumAmount,
                        viewModel::setMinimumAmount,
                        R.string.target_amount,
                    )
                }
                item {
                    DecimalField(
                        state.maximumAmount,
                        viewModel::setMaximumAmount,
                        R.string.current_amount_optional,
                    )
                }
                item { DateField(state.startDate, viewModel::setStartDate, R.string.start_date) }
                item { DateField(state.targetDate, viewModel::setTargetDate, R.string.target_date_optional) }
            }
            FireflyFeature.RULES -> {
                item { RuleGroupSelector(state, viewModel::setRuleGroupId) }
                item { ToggleRow(R.string.rule_enabled, state.active, viewModel::setActive) }
                item { ToggleRow(R.string.rule_match_all, state.strict, viewModel::setStrict) }
                item { ToggleRow(R.string.rule_stop_processing, state.stopProcessing, viewModel::setStopProcessing) }
                item { RuleSectionHeader(R.string.rule_triggers, viewModel::addRuleTrigger) }
                items(state.ruleTriggers.size) { index ->
                    RuleClauseEditor(
                        clause = state.ruleTriggers[index],
                        types = RULE_TRIGGER_TYPES,
                        onType = { viewModel.updateRuleTrigger(index, type = it) },
                        onValue = { viewModel.updateRuleTrigger(index, value = it) },
                        onRemove = { viewModel.removeRuleTrigger(index) },
                    )
                }
                item { RuleSectionHeader(R.string.rule_actions, viewModel::addRuleAction) }
                items(state.ruleActions.size) { index ->
                    RuleClauseEditor(
                        clause = state.ruleActions[index],
                        types = RULE_ACTION_TYPES,
                        onType = { viewModel.updateRuleAction(index, type = it) },
                        onValue = { viewModel.updateRuleAction(index, value = it) },
                        onRemove = { viewModel.removeRuleAction(index) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.notes_optional)) },
                minLines = 3,
            )
        }
        state.error?.let { error ->
            item {
                Text(stringResource(error.messageResource()), color = MaterialTheme.colorScheme.error)
                state.errorDetails?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
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
                            state.itemId != null -> R.string.save_changes
                            else -> state.feature.createLabel()
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun CurrencyField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.currency_code)) },
        supportingText = { Text(stringResource(R.string.iso_currency_help)) },
        singleLine = true,
    )
}

@Composable
private fun DateField(value: String, onValueChange: (String) -> Unit, label: Int) {
    DatePickerField(value, onValueChange, label)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleGroupSelector(state: FeatureEditorUiState, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.ruleGroups.firstOrNull { it.id == state.ruleGroupId }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = selected?.title.orEmpty(), onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            label = { Text(stringResource(R.string.rule_group)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            state.ruleGroups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.title) },
                    onClick = { onSelect(group.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: Int, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        Switch(checked, onChecked)
    }
}

@Composable
private fun RuleSectionHeader(label: Int, onAdd: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleClauseEditor(
    clause: FireflyRuleClause,
    types: List<String>,
    onType: (String) -> Unit,
    onValue: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.small)) {
            ExposedDropdownMenuBox(expanded, { expanded = it }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = clause.type.toFriendlyRuleLabel(), onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text(stringResource(R.string.rule_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                )
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    (types + clause.type).distinct().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.toFriendlyRuleLabel()) },
                            onClick = { onType(type); expanded = false },
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.remove_item))
            }
        }
        OutlinedTextField(
            value = clause.value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.rule_value)) }, singleLine = true,
        )
    }
}

private fun String.toFriendlyRuleLabel(): String = replace('_', ' ').replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelector(state: FeatureEditorUiState, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.accounts.firstOrNull { it.id == state.accountId }
    Column(verticalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.xSmall)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected?.name.orEmpty(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                readOnly = true,
                label = { Text(stringResource(R.string.asset_account)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            onSelect(account.id)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (state.accounts.isEmpty()) {
            Text(
                stringResource(R.string.no_asset_accounts_for_piggy_bank),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun FireflyFeature.nameLabel(): Int = when (this) {
    FireflyFeature.BUDGETS -> R.string.budget_name
    FireflyFeature.CATEGORIES -> R.string.category_name
    FireflyFeature.TAGS -> R.string.tag_name
    FireflyFeature.BILLS -> R.string.bill_name
    FireflyFeature.PIGGY_BANKS -> R.string.piggy_bank_name
    FireflyFeature.RULES -> R.string.rule_name
}

private fun FireflyFeature.createLabel(): Int = when (this) {
    FireflyFeature.BUDGETS -> R.string.create_budget
    FireflyFeature.CATEGORIES -> R.string.create_category
    FireflyFeature.TAGS -> R.string.create_tag
    FireflyFeature.BILLS -> R.string.create_bill
    FireflyFeature.PIGGY_BANKS -> R.string.create_piggy_bank
    FireflyFeature.RULES -> R.string.create_firefly_rule
}

private fun FeatureEditorError.messageResource(): Int = when (this) {
    FeatureEditorError.NAME_REQUIRED -> R.string.feature_name_required
    FeatureEditorError.INVALID_AMOUNT -> R.string.feature_invalid_amount
    FeatureEditorError.MAXIMUM_BELOW_MINIMUM -> R.string.bill_maximum_below_minimum
    FeatureEditorError.CURRENCY_REQUIRED -> R.string.currency_required
    FeatureEditorError.INVALID_CURRENCY -> R.string.invalid_currency_code
    FeatureEditorError.INVALID_DATE -> R.string.feature_invalid_date
    FeatureEditorError.TARGET_DATE_BEFORE_START -> R.string.target_date_before_start
    FeatureEditorError.ACCOUNT_REQUIRED -> R.string.asset_account_required
    FeatureEditorError.SAVE_FAILED -> R.string.feature_save_failed
    FeatureEditorError.LOAD_FAILED -> R.string.feature_load_failed
    FeatureEditorError.RULE_GROUP_REQUIRED -> R.string.rule_group_required
    FeatureEditorError.RULE_CLAUSE_REQUIRED -> R.string.rule_clause_required
}

private fun String.labelResource(): Int = when (this) {
    "weekly" -> R.string.frequency_weekly
    "quarterly" -> R.string.frequency_quarterly
    "half-year" -> R.string.frequency_half_yearly
    "yearly" -> R.string.frequency_yearly
    else -> R.string.frequency_monthly
}

private val REPEAT_FREQUENCIES = listOf("weekly", "monthly", "quarterly", "half-year", "yearly")
private val RULE_TRIGGER_TYPES = listOf(
    "description_contains", "from_account_is", "to_account_is", "amount_more", "amount_less",
    "category_is", "budget_is", "tag_is", "transaction_type", "notes_contains",
)
private val RULE_ACTION_TYPES = listOf(
    "set_category", "set_budget", "add_tag", "set_description", "set_notes",
    "set_source_account", "set_destination_account", "clear_category", "clear_budget", "remove_all_tags",
)
