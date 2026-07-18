/* Presentation-layer Compose screen showing all cached transaction metadata. */
package com.teja.finfly.presentation.transactiondetail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.presentation.components.CategoryPill
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.components.TagPill
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import com.teja.finfly.presentation.theme.creditAmount
import com.teja.finfly.presentation.theme.debitAmount
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency

@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val value = state) {
        TransactionDetailUiState.Loading -> LoadingState()
        TransactionDetailUiState.Error -> ErrorState(onRetry = viewModel::retry)
        TransactionDetailUiState.Empty -> EmptyState(R.string.transaction_not_found, R.string.transaction_load_failed)
        is TransactionDetailUiState.Success -> DetailContent(value, onBack, onEdit)
    }
}

@Composable
private fun DetailContent(
    state: TransactionDetailUiState.Success,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val transaction = state.transaction
    val spacing = FinFlyThemeTokens.spacing
    val locale = LocalConfiguration.current.locales[0]
    var rawSmsExpanded by rememberSaveable(transaction.id) { mutableStateOf(false) }
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    Text(stringResource(R.string.back), modifier = Modifier.padding(start = spacing.small))
                }
                Button(onClick = { onEdit(transaction.id) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Text(stringResource(R.string.edit), modifier = Modifier.padding(start = spacing.small))
                }
            }
        }
        item {
            Text(
                formatAmount(transaction),
                style = MaterialTheme.typography.displaySmall,
                color = when (transaction.type) {
                    TransactionType.WITHDRAWAL -> MaterialTheme.colorScheme.debitAmount
                    TransactionType.DEPOSIT -> MaterialTheme.colorScheme.creditAmount
                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                },
            )
            Text(transaction.description, style = MaterialTheme.typography.headlineMedium)
        }
        item {
            val dateAndTime = formatFullDate(
                transaction,
                stringResource(R.string.transaction_detail_date_pattern),
                stringResource(R.string.transaction_detail_time_pattern),
                locale,
            )
            DetailLine(
                R.string.date_and_time,
                stringResource(R.string.transaction_detail_date_value, dateAndTime.first, dateAndTime.second),
            )
        }
        item { DetailLine(R.string.account, transaction.account.ifBlank { stringResource(R.string.account_unknown) }) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Text(
                    stringResource(R.string.category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    CategoryPill(transaction.category)
                }
            }
        }
        if (transaction.tags.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Text(stringResource(R.string.tags), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) { transaction.tags.forEach { TagPill(it) } }
            }
        }
        state.reference?.let { reference -> item { DetailLine(R.string.reference_number, reference) } }
        item {
            Text(stringResource(R.string.notes), style = MaterialTheme.typography.labelLarge)
            Card(modifier = Modifier.fillMaxWidth().padding(top = spacing.small)) {
                Text(
                    state.displayNotes ?: stringResource(R.string.no_notes),
                    modifier = Modifier.padding(spacing.medium),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        state.rawSms?.let { rawSms ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(spacing.medium)) {
                        TextButton(onClick = { rawSmsExpanded = !rawSmsExpanded }) {
                            Text(
                                stringResource(
                                    if (rawSmsExpanded) R.string.hide_raw_sms else R.string.show_raw_sms
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                if (rawSmsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                            )
                        }
                        if (rawSmsExpanded) {
                            Text(
                                rawSms,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = spacing.small),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: Int, value: String) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatFullDate(
    transaction: Transaction,
    datePattern: String,
    timePattern: String,
    locale: java.util.Locale,
): Pair<String, String> {
    val date = transaction.date.atZone(ZoneId.systemDefault())
    return date.format(DateTimeFormatter.ofPattern(datePattern, locale)) to
        date.format(DateTimeFormatter.ofPattern(timePattern, locale))
}

private fun formatAmount(transaction: Transaction): String = NumberFormat.getCurrencyInstance().run {
    runCatching { currency = Currency.getInstance(transaction.currency) }
    format(if (transaction.type == TransactionType.WITHDRAWAL) transaction.amount.negate() else transaction.amount)
}
