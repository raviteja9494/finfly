/* Presentation-layer Compose screen for compact cash-flow and category reports. */
package com.teja.finfly.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.CategorySpend
import com.teja.finfly.domain.model.MonthlyCashFlow
import com.teja.finfly.domain.model.ReportsSummary
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import com.teja.finfly.presentation.theme.creditAmount
import com.teja.finfly.presentation.theme.debitAmount
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency
import kotlin.math.max

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsContent(state, viewModel::refresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsContent(state: ReportsUiState, onRefresh: () -> Unit) {
    val refreshing = when (state) {
        is ReportsUiState.Success -> state.isRefreshing
        is ReportsUiState.Empty -> state.isRefreshing
        else -> false
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (state) {
            ReportsUiState.Loading -> LoadingState()
            ReportsUiState.Error -> ErrorState(onRetry = onRefresh)
            is ReportsUiState.Empty -> EmptyState(R.string.no_report_data, R.string.no_report_data_message)
            is ReportsUiState.Success -> ReportsList(state.summary)
        }
    }
}

@Composable
private fun ReportsList(summary: ReportsSummary) {
    val spacing = FinFlyThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = spacing.medium,
            end = spacing.medium,
            top = spacing.large,
            bottom = spacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item { ReportsIntro() }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                SummaryCard(
                    title = stringResource(R.string.report_income),
                    amount = summary.monthIncome,
                    currency = summary.currency,
                    amountColor = MaterialTheme.colorScheme.creditAmount,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    title = stringResource(R.string.report_expenses),
                    amount = summary.monthExpenses,
                    currency = summary.currency,
                    amountColor = MaterialTheme.colorScheme.debitAmount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { NetFlowCard(summary.monthNetFlow, summary.currency) }
        item { CashFlowChart(summary.monthlyCashFlow, summary.currency) }
        if (summary.categorySpending.isNotEmpty()) {
            item { CategoryReport(summary.categorySpending, summary.monthExpenses, summary.currency) }
        }
    }
}

@Composable
private fun ReportsIntro() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(FinFlyThemeTokens.radii.chip),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column {
            Text(stringResource(R.string.report_overview), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.report_overview_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: BigDecimal,
    currency: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(FinFlyThemeTokens.spacing.medium)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatCurrency(amount, currency),
                style = MaterialTheme.typography.titleLarge,
                color = amountColor,
                modifier = Modifier.padding(top = FinFlyThemeTokens.spacing.small),
            )
        }
    }
}

@Composable
private fun NetFlowCard(amount: BigDecimal, currency: String) {
    val positive = amount >= BigDecimal.ZERO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(FinFlyThemeTokens.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.report_net_flow),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    stringResource(R.string.report_net_flow_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                formatCurrency(amount, currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (positive) MaterialTheme.colorScheme.creditAmount else MaterialTheme.colorScheme.debitAmount,
            )
        }
    }
}

@Composable
private fun CashFlowChart(rows: List<MonthlyCashFlow>, currency: String) {
    val locale = LocalConfiguration.current.locales[0]
    val maximum = rows.maxOfOrNull { maxOf(it.income, it.expenses) } ?: BigDecimal.ZERO
    val incomeColor = MaterialTheme.colorScheme.creditAmount
    val expenseColor = MaterialTheme.colorScheme.debitAmount
    ReportCard(title = stringResource(R.string.report_cash_flow)) {
        Text(
            stringResource(R.string.report_last_three_months),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChartLegend(incomeColor, expenseColor)
        Row(
            modifier = Modifier.fillMaxWidth().height(172.dp),
            horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
            verticalAlignment = Alignment.Bottom,
        ) {
            rows.forEach { row ->
                MonthBars(
                    row = row,
                    maximum = maximum,
                    incomeColor = incomeColor,
                    expenseColor = expenseColor,
                    monthLabel = row.month.format(
                        DateTimeFormatter.ofPattern(stringResource(R.string.report_month_pattern), locale)
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            stringResource(
                R.string.report_current_month_total,
                formatCurrency(rows.lastOrNull()?.expenses ?: BigDecimal.ZERO, currency),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChartLegend(incomeColor: Color, expenseColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium)) {
        LegendItem(incomeColor, stringResource(R.string.report_income))
        LegendItem(expenseColor, stringResource(R.string.report_expenses))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MonthBars(
    row: MonthlyCashFlow,
    maximum: BigDecimal,
    incomeColor: Color,
    expenseColor: Color,
    monthLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Row(
            modifier = Modifier.height(136.dp),
            horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
            verticalAlignment = Alignment.Bottom,
        ) {
            ReportBar(row.income, maximum, incomeColor)
            ReportBar(row.expenses, maximum, expenseColor)
        }
        Text(monthLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ReportBar(amount: BigDecimal, maximum: BigDecimal, color: Color) {
    val ratio = if (maximum > BigDecimal.ZERO) {
        amount.divide(maximum, 4, RoundingMode.HALF_UP).toFloat()
    } else 0f
    Box(
        Modifier.width(22.dp).height(max(MINIMUM_BAR_HEIGHT, MAXIMUM_BAR_HEIGHT * ratio).dp)
            .background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
    )
}

@Composable
private fun CategoryReport(rows: List<CategorySpend>, totalExpenses: BigDecimal, currency: String) {
    val maximum = rows.maxOfOrNull(CategorySpend::amount) ?: BigDecimal.ZERO
    ReportCard(title = stringResource(R.string.report_top_categories)) {
        rows.forEach { row ->
            val barRatio = row.amount.ratioOf(maximum)
            val percentage = row.amount.ratioOf(totalExpenses) * PERCENT_MULTIPLIER
            Column(verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.xSmall)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        row.category.ifBlank { stringResource(R.string.category_uncategorized) },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(
                            R.string.report_category_amount,
                            percentage.toInt(),
                            formatCurrency(row.amount, currency),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Box(
                    Modifier.fillMaxWidth(barRatio.coerceIn(MINIMUM_BAR_RATIO, 1f)).height(9.dp).background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(FinFlyThemeTokens.radii.chip),
                    )
                )
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(FinFlyThemeTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

private fun BigDecimal.ratioOf(total: BigDecimal): Float = if (total > BigDecimal.ZERO) {
    divide(total, 4, RoundingMode.HALF_UP).toFloat()
} else 0f

private fun formatCurrency(amount: BigDecimal, currency: String): String = NumberFormat.getCurrencyInstance().run {
    runCatching { this.currency = Currency.getInstance(currency) }
    format(amount)
}

private const val MINIMUM_BAR_HEIGHT = 4f
private const val MAXIMUM_BAR_HEIGHT = 128f
private const val MINIMUM_BAR_RATIO = 0.02f
private const val PERCENT_MULTIPLIER = 100f
