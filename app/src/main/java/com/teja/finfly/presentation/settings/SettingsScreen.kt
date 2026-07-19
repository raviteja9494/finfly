/* Presentation-layer Compose screen for grouped Dashboard and Firefly settings. */
package com.teja.finfly.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.CategoryChartStyle
import com.teja.finfly.domain.model.DashboardChartPeriod
import com.teja.finfly.domain.model.DashboardRangeMode
import com.teja.finfly.domain.model.AiModelState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.components.ConfirmationDialog
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val value = state) {
        SettingsUiState.Loading -> LoadingState()
        SettingsUiState.Empty -> SettingsFormContent(SettingsForm(), viewModel)
        SettingsUiState.Error -> ErrorState()
        is SettingsUiState.Success -> SettingsFormContent(value.form, viewModel)
    }
}

@Composable
private fun SettingsFormContent(form: SettingsForm, viewModel: SettingsViewModel) {
    val spacing = FinFlyThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xSmall)) {
                Text(stringResource(R.string.settings_page_title), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.settings_page_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            CollapsibleSettingsSection(
                title = R.string.ai_settings_title,
                description = R.string.ai_settings_description,
                icon = Icons.Rounded.AutoAwesome,
                initiallyExpanded = false,
            ) {
                AiSettings(form, viewModel)
            }
        }
        item {
            CollapsibleSettingsSection(
                title = R.string.timezone_settings,
                description = R.string.timezone_settings_description,
                icon = Icons.Rounded.Schedule,
                initiallyExpanded = false,
            ) {
                SettingsSwitch(
                    title = R.string.use_device_timezone,
                    description = R.string.use_device_timezone_description,
                    checked = form.useDeviceTimezone,
                    onCheckedChange = viewModel::setUseDeviceTimezone,
                )
            }
        }
        item {
            CollapsibleSettingsSection(
                title = R.string.dashboard_settings,
                description = R.string.dashboard_settings_description,
                icon = Icons.Rounded.Dashboard,
                initiallyExpanded = false,
            ) {
                DashboardSettings(form, viewModel)
            }
        }
        item {
            CollapsibleSettingsSection(
                title = R.string.firefly_connection_section,
                description = R.string.settings_subtitle,
                icon = Icons.Rounded.Cloud,
                initiallyExpanded = false,
            ) {
                ConnectionSettings(form, viewModel)
            }
        }
    }
}

@Composable
private fun AiSettings(form: SettingsForm, viewModel: SettingsViewModel) {
    val spacing = FinFlyThemeTokens.spacing
    val config = form.aiConfig
    val uriHandler = LocalUriHandler.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importAiModel(it.toString()) }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.exportAiModel(it.toString()) } }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    if (showDeleteConfirmation) {
        ConfirmationDialog(
            title = R.string.ai_delete_model,
            message = stringResource(R.string.ai_delete_model_confirmation),
            confirmLabel = R.string.delete,
            onConfirm = {
                showDeleteConfirmation = false
                viewModel.deleteAiModel()
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }

    Text(stringResource(R.string.ai_model_status), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.ai_hugging_face_token_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        stringResource(R.string.ai_token_after_download_info),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
    OutlinedTextField(
        value = form.huggingFaceToken,
        onValueChange = viewModel::updateHuggingFaceToken,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.ai_hugging_face_token)) },
        singleLine = true,
        visualTransformation = if (form.showHuggingFaceToken) {
            VisualTransformation.None
        } else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = viewModel::toggleHuggingFaceTokenVisibility) {
                Icon(
                    if (form.showHuggingFaceToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(
                        if (form.showHuggingFaceToken) R.string.hide_token else R.string.show_token
                    ),
                )
            }
        },
    )
    OutlinedButton(
        onClick = { uriHandler.openUri(GEMMA_MODEL_PAGE) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.ai_accept_gemma_license))
    }
    when (val state = form.aiModelState) {
        is AiModelState.NotDownloaded -> {
            Text(stringResource(R.string.ai_model_not_downloaded, state.availableMb))
            Button(onClick = viewModel::downloadAiModel, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Text(stringResource(R.string.ai_download_model), Modifier.padding(start = spacing.small))
            }
        }
        is AiModelState.Downloading -> {
            LinearProgressIndicator(
                progress = { state.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.ai_download_progress, state.downloadedMb, state.totalMb, state.progressPercent))
            OutlinedButton(onClick = viewModel::cancelAiDownload, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
        is AiModelState.Downloaded -> {
            Text(stringResource(R.string.ai_model_downloaded, state.modelSizeGb))
            Text(
                stringResource(R.string.ai_model_location, state.filePath),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AiModelState.Loading -> Text(stringResource(R.string.ai_loading_model))
        AiModelState.Ready -> Text(stringResource(R.string.ai_model_ready))
        is AiModelState.Error -> {
            Text(
                stringResource(
                    if (state.message == "ai_model_access_error") R.string.ai_error_model_access
                    else R.string.ai_error_download
                ),
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = viewModel::downloadAiModel) { Text(stringResource(R.string.retry)) }
        }
    }
    val hasUsableModel = form.aiModelState is AiModelState.Downloaded || form.aiModelState is AiModelState.Ready
    if (!hasUsableModel && form.aiModelState !is AiModelState.Downloading && form.aiModelState !is AiModelState.Loading) {
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.FileOpen, contentDescription = null)
            Text(stringResource(R.string.ai_import_local_model), Modifier.padding(start = spacing.small))
        }
        Text(
            stringResource(R.string.ai_import_local_model_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (hasUsableModel) {
        OutlinedButton(
            onClick = { exportLauncher.launch(GEMMA_MODEL_FILE_NAME) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.SaveAlt, contentDescription = null)
            Text(stringResource(R.string.ai_save_model_copy), Modifier.padding(start = spacing.small))
        }
        Text(
            stringResource(R.string.ai_save_model_copy_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Delete, contentDescription = null)
            Text(stringResource(R.string.ai_delete_model), Modifier.padding(start = spacing.small))
        }
    }
    form.aiFileFeedback?.let { feedback ->
        val message = when (feedback) {
            AiFileFeedback.IMPORTED -> R.string.ai_model_imported
            AiFileFeedback.EXPORTED -> R.string.ai_model_copy_saved
            AiFileFeedback.INVALID_MODEL -> R.string.ai_model_import_invalid
            AiFileFeedback.FAILED -> R.string.ai_model_file_operation_failed
        }
        Text(
            stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = if (feedback == AiFileFeedback.IMPORTED || feedback == AiFileFeedback.EXPORTED) {
                MaterialTheme.colorScheme.primary
            } else MaterialTheme.colorScheme.error,
        )
    }

    ChoiceLabel(R.string.ai_max_transactions)
    Text(stringResource(R.string.ai_max_transactions_value, config.maxTransactions))
    Slider(
        value = config.maxTransactions.toFloat(),
        onValueChange = { viewModel.setAiMaxTransactions((it / 10).toInt() * 10) },
        valueRange = 10f..100f,
        steps = 8,
    )
    ChoiceLabel(R.string.ai_date_range)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        listOf(7, 30, 90).forEach { days ->
            FilterChip(
                selected = config.dateRangeDays == days,
                onClick = { viewModel.setAiDateRangeDays(days) },
                label = { Text(stringResource(R.string.ai_days, days)) },
            )
        }
    }
    SettingsSwitch(
        title = R.string.ai_include_balances,
        description = R.string.ai_include_balances_description,
        checked = config.includeBalances,
        onCheckedChange = viewModel::setAiIncludeBalances,
    )
    SettingsSwitch(
        title = R.string.ai_include_categories,
        description = R.string.ai_include_categories_description,
        checked = config.includeCategories,
        onCheckedChange = viewModel::setAiIncludeCategories,
    )
    SettingsSwitch(
        title = R.string.ai_include_sms_rules,
        description = R.string.ai_include_sms_rules_description,
        checked = config.includeSmsRules,
        onCheckedChange = viewModel::setAiIncludeSmsRules,
    )
    ChoiceLabel(R.string.ai_response_tokens)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        listOf(256, 512, 1024).forEach { tokens ->
            FilterChip(
                selected = config.maxResponseTokens == tokens,
                onClick = { viewModel.setAiMaxResponseTokens(tokens) },
                label = { Text(tokens.toString()) },
            )
        }
    }
    ChoiceLabel(R.string.ai_temperature)
    Text(stringResource(R.string.ai_temperature_value, config.temperature))
    Slider(
        value = config.temperature,
        onValueChange = viewModel::setAiTemperature,
        valueRange = 0.1f..1f,
        steps = 8,
    )
    Text(
        stringResource(R.string.ai_private_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

private const val GEMMA_MODEL_PAGE = "https://huggingface.co/litert-community/Gemma3-1B-IT"
private const val GEMMA_MODEL_FILE_NAME = "gemma3-1b-it-int4.litertlm"

@Composable
private fun CollapsibleSettingsSection(
    title: Int,
    description: Int,
    icon: ImageVector,
    initiallyExpanded: Boolean,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val spacing = FinFlyThemeTokens.spacing
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(spacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xSmall)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.collapse_section else R.string.expand_section
                    ),
                )
            }
            AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider()
                    Column(
                        Modifier.fillMaxWidth().padding(spacing.large),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium),
                    ) { content() }
                }
            }
        }
    }
}

@Composable
private fun DashboardSettings(form: SettingsForm, viewModel: SettingsViewModel) {
    val spacing = FinFlyThemeTokens.spacing
    SettingsSwitch(
        title = R.string.show_spending_insight,
        description = R.string.show_spending_insight_description,
        checked = form.showSpendingInsight,
        onCheckedChange = viewModel::setShowSpendingInsight,
    )
    SettingsSwitch(
        title = R.string.show_net_worth_summary,
        description = R.string.show_net_worth_summary_description,
        checked = form.showNetWorthSummary,
        onCheckedChange = viewModel::setShowNetWorthSummary,
    )
    ChoiceLabel(R.string.recent_transactions_count)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        listOf(5, 10, 20).forEach { count ->
            FilterChip(
                selected = form.recentTransactionsCount == count,
                onClick = { viewModel.setRecentTransactionsCount(count) },
                label = { Text(count.toString()) },
            )
        }
    }
    ChoiceLabel(R.string.spending_chart_period)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        DashboardChartPeriod.entries.forEach { period ->
            FilterChip(
                selected = form.dashboardChartPeriod == period,
                onClick = { viewModel.setDashboardChartPeriod(period) },
                label = { Text(stringResource(period.labelResource())) },
            )
        }
    }
    ChoiceLabel(R.string.spending_chart_range_mode)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        DashboardRangeMode.entries.forEach { mode ->
            FilterChip(
                selected = form.dashboardRangeMode == mode,
                onClick = { viewModel.setDashboardRangeMode(mode) },
                label = { Text(stringResource(mode.labelResource())) },
            )
        }
    }
    Text(
        stringResource(form.rangeDescriptionResource()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ChoiceLabel(R.string.default_category_chart)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        CategoryChartStyle.entries.forEach { style ->
            FilterChip(
                selected = form.categoryChartStyle == style,
                onClick = { viewModel.setCategoryChartStyle(style) },
                label = { Text(stringResource(style.labelResource())) },
            )
        }
    }
    ChoiceLabel(R.string.category_chart_period)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        DashboardChartPeriod.entries.forEach { period ->
            FilterChip(
                selected = form.categoryChartPeriod == period,
                onClick = { viewModel.setCategoryChartPeriod(period) },
                label = { Text(stringResource(period.labelResource())) },
            )
        }
    }
    ChoiceLabel(R.string.category_chart_range_mode)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        DashboardRangeMode.entries.forEach { mode ->
            FilterChip(
                selected = form.categoryRangeMode == mode,
                onClick = { viewModel.setCategoryRangeMode(mode) },
                label = { Text(stringResource(mode.labelResource())) },
            )
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: Int,
    description: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceLabel(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ConnectionSettings(form: SettingsForm, viewModel: SettingsViewModel) {
    val spacing = FinFlyThemeTokens.spacing
    var showLogoutConfirmation by rememberSaveable { mutableStateOf(false) }
    if (showLogoutConfirmation) {
        ConfirmationDialog(
            title = R.string.logout_firefly,
            message = stringResource(R.string.logout_firefly_message),
            confirmLabel = R.string.logout,
            onConfirm = {
                showLogoutConfirmation = false
                viewModel.logout()
            },
            onDismiss = { showLogoutConfirmation = false },
        )
    }
    OutlinedTextField(
        value = form.serverUrl,
        onValueChange = viewModel::updateServerUrl,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.server_url_label)) },
        placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
        leadingIcon = { Icon(Icons.Rounded.Cloud, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
    OutlinedTextField(
        value = form.bearerToken,
        onValueChange = viewModel::updateBearerToken,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.bearer_token_label)) },
        placeholder = { Text(stringResource(R.string.bearer_token_placeholder)) },
        singleLine = true,
        visualTransformation = if (form.showToken) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = viewModel::toggleTokenVisibility) {
                Icon(
                    if (form.showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(if (form.showToken) R.string.hide_token else R.string.show_token),
                )
            }
        },
    )
    Text(
        stringResource(
            R.string.last_sync_label,
            form.lastSyncTime?.atZone(ZoneId.systemDefault())?.format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            ) ?: stringResource(R.string.last_sync_never),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Feedback(form.feedback)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
    ) {
        OutlinedButton(onClick = viewModel::test, enabled = !form.isTesting && !form.isSaving) {
            if (form.isTesting) CircularProgressIndicator(Modifier.padding(end = spacing.small))
            Text(stringResource(R.string.test_connection))
        }
        Button(onClick = viewModel::save, enabled = !form.isSaving && !form.isTesting) {
            if (form.isSaving) CircularProgressIndicator(Modifier.padding(end = spacing.small))
            Text(stringResource(R.string.save_settings))
        }
    }
    if (form.serverUrl.isNotBlank() || form.bearerToken.isNotBlank()) {
        OutlinedButton(
            onClick = { showLogoutConfirmation = true },
            enabled = !form.isSaving && !form.isTesting && !form.isLoggingOut,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (form.isLoggingOut) R.string.logging_out else R.string.logout_firefly))
        }
    }
}

private fun DashboardChartPeriod.labelResource(): Int = when (this) {
    DashboardChartPeriod.WEEK -> R.string.chart_period_week
    DashboardChartPeriod.MONTH -> R.string.chart_period_month
}

private fun DashboardRangeMode.labelResource(): Int = when (this) {
    DashboardRangeMode.CALENDAR -> R.string.chart_range_calendar
    DashboardRangeMode.ROLLING -> R.string.chart_range_rolling
}

private fun CategoryChartStyle.labelResource(): Int = when (this) {
    CategoryChartStyle.BARS -> R.string.chart_style_bars
    CategoryChartStyle.PIE -> R.string.chart_style_pie
}

private fun SettingsForm.rangeDescriptionResource(): Int = when (dashboardChartPeriod to dashboardRangeMode) {
    DashboardChartPeriod.WEEK to DashboardRangeMode.CALENDAR -> R.string.chart_range_calendar_week_description
    DashboardChartPeriod.WEEK to DashboardRangeMode.ROLLING -> R.string.chart_range_rolling_week_description
    DashboardChartPeriod.MONTH to DashboardRangeMode.CALENDAR -> R.string.chart_range_calendar_month_description
    else -> R.string.chart_range_rolling_month_description
}

@Composable
private fun Feedback(feedback: SettingsFeedback?) {
    if (feedback == null) return
    val success = feedback == SettingsFeedback.CONNECTION_SUCCESS || feedback == SettingsFeedback.SAVED ||
        feedback == SettingsFeedback.LOGGED_OUT
    val message = when (feedback) {
        SettingsFeedback.CONNECTION_SUCCESS -> R.string.connection_success
        SettingsFeedback.CONNECTION_FAILED -> R.string.connection_failed
        SettingsFeedback.SAVED -> R.string.settings_saved
        SettingsFeedback.LOGGED_OUT -> R.string.logout_complete
        SettingsFeedback.INVALID_URL -> R.string.settings_invalid_url
        SettingsFeedback.TOKEN_REQUIRED -> R.string.settings_token_required
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
    ) {
        Icon(
            if (success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
            contentDescription = null,
            tint = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        )
        Text(
            text = if (message == R.string.connection_failed) {
                stringResource(message, stringResource(R.string.error_generic))
            } else stringResource(message),
            color = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        )
    }
}
