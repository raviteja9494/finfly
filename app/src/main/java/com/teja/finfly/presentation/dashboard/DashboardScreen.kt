/* Presentation-layer Compose screen for spending totals, recent activity, and sync refresh. */
package com.teja.finfly.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.DashboardSummary
import com.teja.finfly.domain.model.DailySpend
import com.teja.finfly.domain.model.CategorySpend
import com.teja.finfly.domain.model.DashboardChartPeriod
import com.teja.finfly.domain.model.DashboardRangeMode
import com.teja.finfly.domain.model.CategoryChartStyle
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
    onTransactionClick: (String) -> Unit,
    onDaySelected: (DailySpend) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state,
        viewModel::refresh,
        onViewAll,
        onTransactionClick,
        onDaySelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onViewAll: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onDaySelected: (DailySpend) -> Unit,
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
                state.showNetWorthSummary,
                state.showSpendingInsight,
                state.categoryChartStyle,
                onViewAll,
                onTransactionClick,
                onDaySelected,
            )
        }
    }
}

@Composable
private fun DashboardList(
    summary: DashboardSummary,
    showNetWorthSummary: Boolean,
    showSpendingInsight: Boolean,
    categoryChartStyle: CategoryChartStyle,
    onViewAll: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onDaySelected: (DailySpend) -> Unit,
) {
    val spacing = FinFlyThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = spacing.medium, end = spacing.medium, top = spacing.large, bottom = spacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        if (showSpendingInsight) item { DashboardInsight(summary) }
        if (showNetWorthSummary) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium), modifier = Modifier.fillMaxWidth()) {
                    SpendCard(R.string.total_assets, summary.totalAssets, summary.currency, Modifier.weight(1f))
                    SpendCard(R.string.total_liabilities, summary.totalLiabilities, summary.currency, Modifier.weight(1f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium), modifier = Modifier.fillMaxWidth()) {
                SpendCard(R.string.today_spend, summary.todaySpend, summary.currency, Modifier.weight(1f))
                SpendCard(R.string.month_spend, summary.monthSpend, summary.currency, Modifier.weight(1f))
            }
        }
        item {
            SpendingChart(
                spending = summary.chartSpending,
                currency = summary.currency,
                period = summary.chartPeriod,
                rangeMode = summary.rangeMode,
                onDaySelected = onDaySelected,
            )
        }
        if (summary.categorySpending.isNotEmpty()) {
            item { CategorySpendingChart(summary.categorySpending, summary.currency, categoryChartStyle) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onViewAll) { Text(stringResource(R.string.view_all)) }
            }
        }
        items(summary.recentTransactions, key = { it.id }) { transaction ->
            TransactionRow(transaction, Modifier.clickable { onTransactionClick(transaction.id) })
        }
    }
}

@Composable
private fun SpendingChart(
    spending: List<DailySpend>,
    currency: String,
    period: DashboardChartPeriod,
    rangeMode: DashboardRangeMode,
    onDaySelected: (DailySpend) -> Unit,
) {
    val spacing = FinFlyThemeTokens.spacing
    val locale = LocalConfiguration.current.locales[0]
    val maximum = spending.maxOfOrNull { it.amount } ?: BigDecimal.ZERO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(spacing.medium)) {
            Text(
                stringResource(chartTitleResource(period, rangeMode)),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                formatCurrency(spending.fold(BigDecimal.ZERO) { total, day -> total + day.amount }, currency),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = spacing.small),
            )
            if (spending.size <= 7) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(178.dp).padding(top = spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    spending.forEach { day ->
                        SpendingBar(
                            day,
                            maximum,
                            locale,
                            { onDaySelected(day) },
                            Modifier.weight(1f),
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(178.dp).padding(top = spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    items(spending, key = DailySpend::date) { day ->
                        SpendingBar(
                            day,
                            maximum,
                            locale,
                            { onDaySelected(day) },
                            Modifier.width(44.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingBar(
    day: DailySpend,
    maximum: BigDecimal,
    locale: java.util.Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = if (maximum > BigDecimal.ZERO) {
        day.amount.divide(maximum, 4, java.math.RoundingMode.HALF_UP).toFloat()
    } else 0f
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(formatCompactAmount(day.amount), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Box(
            Modifier.fillMaxWidth().height(max(4f, 104f * ratio).dp).background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            )
        )
        Text(
            day.date.format(DateTimeFormatter.ofPattern(stringResource(R.string.chart_day_pattern), locale)),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = FinFlyThemeTokens.spacing.small),
        )
    }
}

private fun chartTitleResource(period: DashboardChartPeriod, mode: DashboardRangeMode): Int =
    when (period to mode) {
        DashboardChartPeriod.WEEK to DashboardRangeMode.CALENDAR -> R.string.chart_calendar_week
        DashboardChartPeriod.WEEK to DashboardRangeMode.ROLLING -> R.string.chart_rolling_week
        DashboardChartPeriod.MONTH to DashboardRangeMode.CALENDAR -> R.string.chart_calendar_month
        else -> R.string.chart_rolling_month
    }

@Composable
private fun CategorySpendingChart(
    spending: List<CategorySpend>,
    currency: String,
    initialStyle: CategoryChartStyle,
) {
    val spacing = FinFlyThemeTokens.spacing
    var mode by rememberSaveable(initialStyle) { mutableStateOf(initialStyle) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.month_by_category),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        mode = if (mode == CategoryChartStyle.BARS) CategoryChartStyle.PIE else CategoryChartStyle.BARS
                    }
                ) {
                    Icon(
                        if (mode == CategoryChartStyle.BARS) Icons.Rounded.DonutLarge else Icons.Rounded.BarChart,
                        contentDescription = stringResource(
                            if (mode == CategoryChartStyle.BARS) R.string.show_pie_chart else R.string.show_bar_chart
                        ),
                    )
                }
            }
            when (mode) {
                CategoryChartStyle.BARS -> CategoryBars(spending, currency)
                CategoryChartStyle.PIE -> CategoryPie(spending, currency)
            }
        }
    }
}

@Composable
private fun CategoryBars(spending: List<CategorySpend>, currency: String) {
    val maximum = spending.maxOfOrNull(CategorySpend::amount) ?: BigDecimal.ZERO
    Column(verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium)) {
        spending.forEach { row ->
            val ratio = if (maximum > BigDecimal.ZERO) {
                row.amount.divide(maximum, 4, java.math.RoundingMode.HALF_UP).toFloat()
            } else 0f
            Column(verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.xSmall)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        row.category.ifBlank { stringResource(R.string.category_uncategorized) },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(formatCurrency(row.amount, currency), style = MaterialTheme.typography.labelLarge)
                }
                Box(
                    Modifier.fillMaxWidth(ratio.coerceIn(0.02f, 1f)).height(10.dp).background(
                        MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(FinFlyThemeTokens.radii.chip),
                    )
                )
            }
        }
    }
}

@Composable
private fun CategoryPie(spending: List<CategorySpend>, currency: String) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val total = spending.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
    ) {
        Canvas(Modifier.size(180.dp)) {
            var startAngle = -90f
            spending.forEachIndexed { index, row ->
                val sweep = if (total > BigDecimal.ZERO) {
                    row.amount.divide(total, 6, java.math.RoundingMode.HALF_UP).toFloat() * 360f
                } else 0f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                )
                startAngle += sweep
            }
        }
        spending.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
            ) {
                Box(Modifier.size(12.dp).background(colors[index % colors.size], RoundedCornerShape(4.dp)))
                Text(
                    row.category.ifBlank { stringResource(R.string.category_uncategorized) },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(formatCurrency(row.amount, currency), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DashboardInsight(summary: DashboardSummary) {
    val spacing = FinFlyThemeTokens.spacing
    Box(
        modifier = Modifier.fillMaxWidth().height(176.dp).background(
            brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer)),
            shape = RoundedCornerShape(FinFlyThemeTokens.radii.hero),
        ).padding(spacing.large),
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Rounded.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(spacing.medium))
            Text(stringResource(R.string.spending_pace), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(
                    R.string.daily_average_value,
                    formatCurrency(summary.monthDailyAverage, summary.currency),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(
                    R.string.top_category_value,
                    summary.categorySpending.firstOrNull()?.category
                        ?.takeIf(String::isNotBlank) ?: stringResource(R.string.category_uncategorized),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

private fun formatCompactAmount(amount: BigDecimal): String = NumberFormat.getNumberInstance().run {
    maximumFractionDigits = 0
    format(amount)
}
