/* Presentation-layer reusable card row for a normalized transaction. */
package com.teja.finfly.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teja.finfly.R
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import com.teja.finfly.presentation.theme.creditAmount
import com.teja.finfly.presentation.theme.debitAmount
import java.text.NumberFormat
import java.time.ZoneId
import com.teja.finfly.presentation.theme.LocalFinFlyZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency

@Composable
fun TransactionRow(transaction: Transaction, modifier: Modifier = Modifier) {
    val spacing = FinFlyThemeTokens.spacing
    val locale = LocalConfiguration.current.locales[0]
    val amountColor = when (transaction.type) {
        TransactionType.WITHDRAWAL -> MaterialTheme.colorScheme.debitAmount
        TransactionType.DEPOSIT -> MaterialTheme.colorScheme.creditAmount
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.WITHDRAWAL -> Icons.Rounded.ArrowUpward
                            TransactionType.DEPOSIT -> Icons.Rounded.ArrowDownward
                            TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = transaction.account.ifBlank { stringResource(R.string.account_unknown) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = spacing.xSmall),
                    )
                }
                Text(
                    text = formatAmount(transaction),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }
            if (transaction.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
                ) {
                    transaction.tags.forEach { TagPill(it, showKind = true) }
                }
            }
            CategoryPill(transaction.category, showKind = true)
            Text(
                text = transaction.date.atZone(LocalFinFlyZoneId.current).format(
                    DateTimeFormatter.ofPattern(
                        stringResource(R.string.transaction_card_date_pattern),
                        locale,
                    )
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatAmount(transaction: Transaction): String {
    val formatter = NumberFormat.getCurrencyInstance()
    runCatching { formatter.currency = Currency.getInstance(transaction.currency) }
    val signed = if (transaction.type == TransactionType.WITHDRAWAL) transaction.amount.negate() else transaction.amount
    return formatter.format(signed)
}
