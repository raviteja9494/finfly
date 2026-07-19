/* Presentation-layer Compose list and detail for SMS processing history. */
package com.teja.finfly.presentation.smsrules

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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.SmsLog
import com.teja.finfly.domain.model.SmsLogResult
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SmsLogsScreen(
    onLogClick: (String) -> Unit,
    viewModel: SmsLogsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val value = state) {
        SmsLogsUiState.Loading -> LoadingState()
        SmsLogsUiState.Error -> ErrorState()
        SmsLogsUiState.Empty -> EmptyState(R.string.no_sms_logs, R.string.no_sms_logs_message)
        is SmsLogsUiState.Success -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(FinFlyThemeTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
        ) {
            items(value.logs, key = SmsLog::id) { log -> SmsLogCard(log) { onLogClick(log.id) } }
        }
    }
}

@Composable
private fun SmsLogCard(log: SmsLog, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            Modifier.padding(FinFlyThemeTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.xSmall),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(log.sender, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                ResultChip(log.result)
            }
            Text(log.message.take(50), maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTimestamp(log.timestamp), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SmsLogDetailScreen(
    onBack: () -> Unit,
    onCreateRule: (String) -> Unit,
    viewModel: SmsLogDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val value = state) {
        SmsLogDetailUiState.Loading -> LoadingState()
        SmsLogDetailUiState.Error -> ErrorState()
        SmsLogDetailUiState.Empty -> EmptyState(R.string.sms_log_not_found, R.string.sms_log_not_found_message)
        is SmsLogDetailUiState.Success -> SmsLogDetail(value.log, onBack, onCreateRule)
    }
}

@Composable
private fun SmsLogDetail(log: SmsLog, onBack: () -> Unit, onCreateRule: (String) -> Unit) {
    val spacing = FinFlyThemeTokens.spacing
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            OutlinedButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.back), Modifier.padding(start = spacing.small))
            }
        }
        item { DetailLine(R.string.sms_sender, log.sender) }
        item { DetailLine(R.string.sms_timestamp, formatTimestamp(log.timestamp)) }
        item {
            Text(stringResource(R.string.sms_result), style = MaterialTheme.typography.labelLarge)
            ResultChip(log.result)
        }
        item { DetailLine(R.string.sms_reason, stringResource(log.reason.reasonResource())) }
        item { DetailLine(R.string.matched_rule, log.matchedRule.ifBlank { stringResource(R.string.no_rule_matched) }) }
        item {
            Text(stringResource(R.string.full_sms_message), style = MaterialTheme.typography.labelLarge)
            Card(Modifier.fillMaxWidth().padding(top = spacing.small)) {
                Text(log.message, Modifier.padding(spacing.medium))
            }
        }
        if (log.result == SmsLogResult.NO_RULE) item {
            Button(onClick = { onCreateRule(log.sender) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.create_rule_for_sender))
            }
        }
    }
}

@Composable
private fun ResultChip(result: SmsLogResult) {
    val color = when (result) {
        SmsLogResult.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
        SmsLogResult.SKIPPED -> MaterialTheme.colorScheme.secondaryContainer
        SmsLogResult.NO_RULE -> MaterialTheme.colorScheme.errorContainer
    }
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(stringResource(result.labelResource()), Modifier.padding(horizontal = FinFlyThemeTokens.spacing.small))
    }
}

@Composable
private fun DetailLine(label: Int, value: String) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun SmsLogResult.labelResource(): Int = when (this) {
    SmsLogResult.SUCCESS -> R.string.sms_result_success
    SmsLogResult.SKIPPED -> R.string.sms_result_skipped
    SmsLogResult.NO_RULE -> R.string.sms_result_no_rule
}

private fun String.reasonResource(): Int = when (this) {
    "transaction_created" -> R.string.sms_reason_created
    "no_rule_matched" -> R.string.no_rule_matched
    "account_not_mapped" -> R.string.sms_reason_account_not_mapped
    "firefly_save_failed" -> R.string.sms_reason_firefly_failed
    "type_not_found" -> R.string.parse_type_not_found
    "amount_not_found" -> R.string.parse_amount_not_found
    "description_not_found" -> R.string.parse_description_not_found
    "rules_unavailable" -> R.string.sms_reason_rules_unavailable
    else -> R.string.sms_reason_unknown
}

private fun formatTimestamp(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
