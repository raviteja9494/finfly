/* Presentation-layer Compose screen for spending totals, recent activity, and sync refresh. */
package com.teja.finfly.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.DashboardSummary
import com.teja.finfly.domain.model.DailySpend
import com.teja.finfly.presentation.accounts.AccountCard
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.components.TransactionRow
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun DashboardScreen(
    onViewAll: () -> Unit,
    onManageAccounts: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(state, viewModel::refresh, onViewAll, onManageAccounts, onTransactionClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onViewAll: () -> Unit,
    onManageAccounts: () -> Unit,
    onTransactionClick: (String) -> Unit,
) {
    val refreshing = when (state) {
        is DashboardUiState.Success -> state.isRefreshing
        is DashboardUiState.Empty -> state.isRefreshing
        else -> false
    }
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        when (state) {
            DashboardUiState.Loading -> LoadingState()
            is DashboardUiState.Error -> ErrorState(onRetry = onRefresh)
            is DashboardUiState.Empty -> EmptyState(R.string.no_dashboard_data, R.string.no_dashboard_data_message)
            is DashboardUiState.Success -> DashboardList(
                state.summary,
                onViewAll,
                onManageAccounts,
                onTransactionClick,
            )
        }
    }
}

@Composable
private fun DashboardList(
    summary: DashboardSummary,
    onViewAll: () -> Unit,
    onManageAccounts: () -> Unit,
    onTransactionClick: (String) -> Unit,
) {
    val spacing = FinFlyThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = spacing.medium, end = spacing.medium, top = spacing.large, bottom = spacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item { DashboardHero() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium), modifier = Modifier.fillMaxWidth()) {
                SpendCard(R.string.today_spend, summary.todaySpend, summary.currency, Modifier.weight(1f))
                SpendCard(R.string.month_spend, summary.monthSpend, summary.currency, Modifier.weight(1f))
            }
        }
        item { WeeklySpendingChart(summary.weeklySpending, summary.currency) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.bank_accounts), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onManageAccounts) { Text(stringResource(R.string.manage)) }
            }
        }
        if (summary.accounts.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(spacing.medium)) {
                        Text(stringResource(R.string.no_bank_accounts), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onManageAccounts) { Text(stringResource(R.string.add_bank_account)) }
                    }
                }
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    items(summary.accounts, key = { it.id }) { account ->
                        AccountCard(account, Modifier.width(260.dp))
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onViewAll) { Text(stringResource(R.string.view_all)) }
            }
        }
        items(summary.recentTransactions, key = { it.id }) { transaction ->
            TransactionRow(transaction, Modifier.clickable { onTransactionClick(transaction.id) })
        }
    }
}

@Composable
private fun WeeklySpendingChart(spending: List<DailySpend>, currency: String) {
    val spacing = FinFlyThemeTokens.spacing
    val maximum = spending.maxOfOrNull { it.amount } ?: BigDecimal.ZERO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(spacing.medium)) {
            Text(stringResource(R.string.this_week), style = MaterialTheme.typography.titleLarge)
            Text(
                formatCurrency(spending.fold(BigDecimal.ZERO) { total, day -> total + day.amount }, currency),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = spacing.small),
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(148.dp).padding(top = spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.Bottom,
            ) {
                spending.forEach { day ->
                    val ratio = if (maximum > BigDecimal.ZERO) {
                        day.amount.divide(maximum, 4, java.math.RoundingMode.HALF_UP).toFloat()
                    } else 0f
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(max(4f, 108f * ratio).dp).background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            )
                        )
                        Text(
                            day.date.format(DateTimeFormatter.ofPattern("EEE")),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = spacing.small),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHero() {
    val spacing = FinFlyThemeTokens.spacing
    Box(
        modifier = Modifier.fillMaxWidth().height(176.dp).background(
            brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer)),
            shape = RoundedCornerShape(FinFlyThemeTokens.radii.hero),
        ).padding(spacing.large),
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Rounded.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(spacing.medium))
            Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.dashboard_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpendCard(title: Int, amount: BigDecimal, currency: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(FinFlyThemeTokens.spacing.medium)) {
            Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatCurrency(amount, currency), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun formatCurrency(amount: BigDecimal, currency: String): String = NumberFormat.getCurrencyInstance().run {
    runCatching { this.currency = Currency.getInstance(currency) }
    format(amount)
}
