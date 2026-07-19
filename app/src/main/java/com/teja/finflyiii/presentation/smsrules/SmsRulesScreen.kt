/* Presentation-layer Compose hub for SMS permission, rules, transfer, and logs. */
package com.teja.finflyiii.presentation.smsrules

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teja.finflyiii.R
import com.teja.finflyiii.domain.model.BankRule
import com.teja.finflyiii.domain.model.CategoryRule
import com.teja.finflyiii.domain.model.RulesImportMode
import com.teja.finflyiii.presentation.components.ErrorState
import com.teja.finflyiii.presentation.components.LoadingState
import com.teja.finflyiii.presentation.components.DatePickerField
import com.teja.finflyiii.presentation.components.ConfirmationDialog
import com.teja.finflyiii.presentation.theme.FinFlyIIIThemeTokens

@Composable
fun SmsRulesScreen(
    onAddBankRule: () -> Unit,
    onEditBankRule: (String) -> Unit,
    onAddCategoryRule: () -> Unit,
    onEditCategoryRule: (String) -> Unit,
    onOpenLogs: () -> Unit,
    viewModel: SmsRulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember { mutableStateOf(context.hasSmsPermission()) }
    var readPermissionGranted by remember { mutableStateOf(context.hasReadSmsPermission()) }
    var confirmPush by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        viewModel.setEnabled(it)
    }
    val readPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        readPermissionGranted = it
        if (it) viewModel.scanInbox()
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.previewImport(it.toString()) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasSmsPermission()
                readPermissionGranted = context.hasReadSmsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val snackbarHost = remember { SnackbarHostState() }
    val exportedMessage = stringResource(R.string.rules_exported)
    val importedMessage = stringResource(R.string.rules_imported)
    val exportFailed = stringResource(R.string.rules_export_failed)
    val importFailed = stringResource(R.string.rules_import_failed)
    val scanComplete = stringResource(R.string.sms_scan_complete)
    val transactionsPushed = stringResource(R.string.sms_transactions_pushed)
    LaunchedEffect(state.feedback) {
        val message = when (val feedback = state.feedback) {
            is SmsRulesFeedback.Exported -> "$exportedMessage ${feedback.path}"
            is SmsRulesFeedback.Imported -> String.format(importedMessage, feedback.banks, feedback.categories)
            SmsRulesFeedback.ExportFailed -> exportFailed
            SmsRulesFeedback.ImportFailed -> importFailed
            is SmsRulesFeedback.ScanComplete -> String.format(scanComplete, feedback.count)
            is SmsRulesFeedback.TransactionsPushed -> String.format(transactionsPushed, feedback.count)
            null -> null
        }
        if (message != null) {
            snackbarHost.showSnackbar(message)
            viewModel.consumeFeedback()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            state.error -> ErrorState(modifier = Modifier.padding(padding))
            else -> SmsRulesContent(
                state = state,
                permissionGranted = permissionGranted,
                onToggleMaster = { enabled ->
                    if (enabled && !permissionGranted) permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                    else viewModel.setEnabled(enabled)
                },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    )
                },
                onAddBankRule = onAddBankRule,
                onEditBankRule = onEditBankRule,
                onToggleBank = viewModel::toggleBankRule,
                onAddCategoryRule = onAddCategoryRule,
                onEditCategoryRule = onEditCategoryRule,
                onToggleCategory = viewModel::toggleCategoryRule,
                onExport = viewModel::exportRules,
                onImport = { importLauncher.launch(arrayOf(JSON_MIME, TEXT_MIME)) },
                onOpenLogs = onOpenLogs,
                onAddUniversalTag = viewModel::addUniversalTag,
                onRemoveUniversalTag = viewModel::removeUniversalTag,
                onScanFromDate = viewModel::setScanFromDate,
                onScanUntilDate = viewModel::setScanUntilDate,
                onScan = {
                    if (readPermissionGranted) viewModel.scanInbox()
                    else readPermissionLauncher.launch(Manifest.permission.READ_SMS)
                },
                onTogglePreview = viewModel::toggleScanPreview,
                onClearPreview = viewModel::clearScanPreview,
                onRequestPush = { confirmPush = true },
                modifier = Modifier.padding(padding),
            )
        }
    }
    if (confirmPush) {
        val count = state.scanPreview.count {
            it.selected && it.status != SmsPreviewStatus.PUSHED
        }
        ConfirmationDialog(
            title = R.string.push_sms_transactions,
            message = stringResource(R.string.push_sms_transactions_message, count),
            confirmLabel = R.string.push_to_firefly,
            onConfirm = { confirmPush = false; viewModel.pushSelectedPreview() },
            onDismiss = { confirmPush = false },
        )
    }
    state.importPreview?.let { config ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImport,
            title = { Text(stringResource(R.string.import_rules_preview_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.import_rules_preview_message,
                        config.bankRules.size,
                        config.categoryRules.size,
                    )
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.importRules(RulesImportMode.MERGE) }) {
                    Text(stringResource(R.string.merge_rules))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.importRules(RulesImportMode.REPLACE) }) {
                        Text(stringResource(R.string.replace_rules))
                    }
                    TextButton(onClick = viewModel::dismissImport) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
}

@Composable
private fun SmsRulesContent(
    state: SmsRulesUiState,
    permissionGranted: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onAddBankRule: () -> Unit,
    onEditBankRule: (String) -> Unit,
    onToggleBank: (BankRule, Boolean) -> Unit,
    onAddCategoryRule: () -> Unit,
    onEditCategoryRule: (String) -> Unit,
    onToggleCategory: (CategoryRule, Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenLogs: () -> Unit,
    onAddUniversalTag: (String) -> Unit,
    onRemoveUniversalTag: (String) -> Unit,
    onScanFromDate: (String) -> Unit,
    onScanUntilDate: (String) -> Unit,
    onScan: () -> Unit,
    onTogglePreview: (String) -> Unit,
    onClearPreview: () -> Unit,
    onRequestPush: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = FinFlyIIIThemeTokens.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.enable_sms_parsing), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.enable_sms_parsing_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(state.enabled && permissionGranted, onToggleMaster)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        Icon(
                            if (permissionGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                            contentDescription = null,
                            tint = if (permissionGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            stringResource(
                                if (permissionGranted) R.string.sms_permission_granted else R.string.sms_permission_denied
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (!permissionGranted) OutlinedButton(onClick = onOpenSettings) {
                            Text(stringResource(R.string.open_app_settings))
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    Text(stringResource(R.string.universal_parsing_tags), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.universal_parsing_tags_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChipInput(
                        R.string.firefly_tags,
                        R.string.firefly_tags_hint,
                        state.universalTags,
                        onAddUniversalTag,
                        onRemoveUniversalTag,
                    )
                }
            }
        }
        item {
            OnDemandScanCard(
                state = state,
                onFromDate = onScanFromDate,
                onUntilDate = onScanUntilDate,
                onScan = onScan,
                onClear = onClearPreview,
                onPush = onRequestPush,
            )
        }
        items(state.scanPreview, key = OnDemandSmsPreview::id) { preview ->
            SmsPreviewCard(preview, { onTogglePreview(preview.id) })
        }
        item { RulesHeader(R.string.bank_rules, onAddBankRule) }
        items(state.bankRules, key = BankRule::id) { rule ->
            BankRuleCard(rule, { onEditBankRule(rule.id) }, { onToggleBank(rule, it) })
        }
        item { RulesHeader(R.string.category_rules, onAddCategoryRule) }
        items(state.categoryRules, key = CategoryRule::id) { rule ->
            CategoryRuleCard(rule, { onEditCategoryRule(rule.id) }, { onToggleCategory(rule, it) })
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                OutlinedButton(onClick = onExport, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null)
                    Text(stringResource(R.string.export_rules), Modifier.padding(start = spacing.xSmall))
                }
                OutlinedButton(onClick = onImport, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                    Text(stringResource(R.string.import_rules), Modifier.padding(start = spacing.xSmall))
                }
            }
        }
        item {
            Button(onClick = onOpenLogs, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.History, contentDescription = null)
                Text(stringResource(R.string.sms_log), Modifier.padding(start = spacing.small))
            }
        }
    }
}

@Composable
private fun RulesHeader(title: Int, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = FinFlyIIIThemeTokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add)) }
    }
}

@Composable
private fun BankRuleCard(rule: BankRule, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(FinFlyIIIThemeTokens.spacing.medium), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text(rule.senderIds.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (rule.fireflyTags.isNotEmpty()) Text(
                    stringResource(R.string.parsed_tags_value, rule.fireflyTags.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(rule.enabled, onToggle)
        }
    }
}

@Composable
private fun CategoryRuleCard(rule: CategoryRule, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(FinFlyIIIThemeTokens.spacing.medium), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.keyword_count, rule.keywords.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (rule.fireflyCategory.isNotBlank()) Text(
                    stringResource(R.string.parsed_category_value, rule.fireflyCategory),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (rule.fireflyTags.isNotEmpty()) Text(
                    stringResource(R.string.parsed_tags_value, rule.fireflyTags.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(rule.enabled, onToggle)
        }
    }
}

@Composable
private fun OnDemandScanCard(
    state: SmsRulesUiState,
    onFromDate: (String) -> Unit,
    onUntilDate: (String) -> Unit,
    onScan: () -> Unit,
    onClear: () -> Unit,
    onPush: () -> Unit,
) {
    val spacing = FinFlyIIIThemeTokens.spacing
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(stringResource(R.string.on_demand_sms_scan), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.on_demand_sms_scan_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                DatePickerField(state.scanFromDate, onFromDate, R.string.report_from_date, Modifier.weight(1f))
                DatePickerField(state.scanUntilDate, onUntilDate, R.string.report_until_date, Modifier.weight(1f))
            }
            state.scanError?.let {
                Text(stringResource(it.messageResource()), color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onScan, enabled = !state.isScanning, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Text(
                    stringResource(if (state.isScanning) R.string.scanning_sms else R.string.scan_sms),
                    Modifier.padding(start = spacing.small),
                )
            }
            if (state.scanPreview.isNotEmpty()) {
                Text(
                    stringResource(R.string.sms_preview_count, state.scanPreview.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.clear_preview))
                    }
                    Button(
                        onClick = onPush,
                        enabled = !state.isPushingPreview && state.scanPreview.any {
                            it.selected && it.status != SmsPreviewStatus.PUSHED
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(if (state.isPushingPreview) R.string.saving else R.string.review_and_push))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsPreviewCard(preview: OnDemandSmsPreview, onToggle: () -> Unit) {
    val transaction = preview.transaction
    val selectable = preview.status != SmsPreviewStatus.PUSHED
    val category = transaction.category.ifBlank { stringResource(R.string.no_category) }
    val tags = transaction.tags.joinToString().ifBlank { stringResource(R.string.no_tags) }
    Card(Modifier.fillMaxWidth().then(if (selectable) Modifier.clickable(onClick = onToggle) else Modifier)) {
        Row(
            Modifier.padding(FinFlyIIIThemeTokens.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                preview.selected,
                onCheckedChange = if (selectable) { { _: Boolean -> onToggle() } } else null,
            )
            Column(Modifier.weight(1f)) {
                Text(transaction.description, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.sms_preview_amount_and_type, transaction.amount, transaction.type.name),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.parsed_account_value, transaction.accountName), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(
                        R.string.parsed_category_value,
                        category,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(
                        R.string.parsed_tags_value,
                        tags,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (transaction.reference.isNotBlank()) Text(
                    stringResource(R.string.parsed_reference_value, transaction.reference),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(preview.status.labelResource()),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (preview.status) {
                        SmsPreviewStatus.FAILED -> MaterialTheme.colorScheme.error
                        SmsPreviewStatus.DUPLICATE -> MaterialTheme.colorScheme.tertiary
                        SmsPreviewStatus.PUSHED -> MaterialTheme.colorScheme.primary
                        SmsPreviewStatus.READY -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                preview.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun SmsPreviewStatus.labelResource(): Int = when (this) {
    SmsPreviewStatus.READY -> R.string.sms_status_ready
    SmsPreviewStatus.DUPLICATE -> R.string.sms_status_duplicate
    SmsPreviewStatus.PUSHED -> R.string.sms_status_pushed
    SmsPreviewStatus.FAILED -> R.string.sms_status_failed
}

private fun OnDemandScanError.messageResource(): Int = when (this) {
    OnDemandScanError.INVALID_DATE -> R.string.report_invalid_date
    OnDemandScanError.INVALID_RANGE -> R.string.report_invalid_range
    OnDemandScanError.READ_FAILED -> R.string.sms_scan_failed
    OnDemandScanError.PUSH_FAILED -> R.string.sms_push_failed
}

private fun android.content.Context.hasSmsPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

private fun android.content.Context.hasReadSmsPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

private const val JSON_MIME = "application/json"
private const val TEXT_MIME = "text/json"
