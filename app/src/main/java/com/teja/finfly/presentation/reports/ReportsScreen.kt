/* Presentation-layer Compose screen for compact cash-flow and category reports. */
package com.teja.finfly.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.teja.finfly.presentation.components.DatePickerField
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
    ReportsContent(
        state = state,
        onRefresh = viewModel::refresh,
        onFromDateChange = viewModel::setFromDate,
        onUntilDateChange = viewModel::setUntilDate,
        onCategoryChange = viewModel::setCategory,
        onTagChange = viewModel::setTag,
        onApplyFilters = viewModel::applyFilters,
        onClearFilters = viewModel::clearFilters,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsContent(
    state: ReportsUiState,
    onRefresh: () -> Unit,
    onFromDateChange: (String) -> Unit,
    onUntilDateChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
) {
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
            is ReportsUiState.Empty -> ReportsEmpty(
                state.filterForm,
                onFromDateChange,
                onUntilDateChange,
                onCategoryChange,
                onTagChange,
                onApplyFilters,
                onClearFilters,
            )
            is ReportsUiState.Success -> ReportsList(
                state.summary,
                state.filterForm,
                onFromDateChange,
                onUntilDateChange,
                onCategoryChange,
                onTagChange,
                onApplyFilters,
                onClearFilters,
            )
        }
    }
}

@Composable
private fun ReportsList(
    summary: ReportsSummary,
    filterForm: ReportsFilterForm,
    onFromDateChange: (String) -> Unit,
    onUntilDateChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
) {
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
            ReportFilters(
                filterForm,
                onFromDateChange,
                onUntilDateChange,
                onCategoryChange,
                onTagChange,
                onApplyFilters,
                onClearFilters,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                SummaryCard(
                    title = stringResource(R.string.report_income),
                    amount = summary.income,
                    currency = summary.currency,
                    amountColor = MaterialTheme.colorScheme.creditAmount,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    title = stringResource(R.string.report_expenses),
                    amount = summary.expenses,
                    currency = summary.currency,
                    amountColor = MaterialTheme.colorScheme.debitAmount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { NetFlowCard(summary.netFlow, summary.currency) }
        item { CashFlowChart(summary, summary.currency) }
        if (summary.categorySpending.isNotEmpty()) {
            item { CategoryReport(summary.categorySpending, summary.expenses, summary.currency) }
        }
    }
}

@Composable
private fun ReportsEmpty(
    form: ReportsFilterForm,
    onFromDateChange: (String) -> Unit,
    onUntilDateChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(FinFlyThemeTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
    ) {
        ReportFilters(
            form,
            onFromDateChange,
            onUntilDateChange,
            onCategoryChange,
            onTagChange,
            onApplyFilters,
            onClearFilters,
        )
        EmptyState(
            R.string.no_report_data,
            R.string.no_report_data_message,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFilters(
    form: ReportsFilterForm,
    onFromDateChange: (String) -> Unit,
    onUntilDateChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val spacing = FinFlyThemeTokens.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FinFlyThemeTokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Icon(Icons.Rounded.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.report_filters_count, form.activeCount),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(
                            R.string.report_filter_summary,
                            form.appliedFilter.fromDate.toString(),
                            form.appliedFilter.untilDate.toString(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
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
            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = spacing.medium,
                        end = spacing.medium,
                        bottom = spacing.medium,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        DatePickerField(
                            value = form.fromDate,
                            onValueChange = onFromDateChange,
                            modifier = Modifier.weight(1f),
                            label = R.string.report_from_date,
                        )
                        DatePickerField(
                            value = form.untilDate,
                            onValueChange = onUntilDateChange,
                            modifier = Modifier.weight(1f),
                            label = R.string.report_until_date,
                        )
                    }
                    ReportDropdown(
                        label = stringResource(R.string.category),
                        allLabel = stringResource(R.string.all_categories),
                        choices = form.categories.map { it.name },
                        selected = form.category,
                        onSelected = onCategoryChange,
                    )
                    ReportDropdown(
                        label = stringResource(R.string.tags),
                        allLabel = stringResource(R.string.all_tags),
                        choices = form.tags.map { it.name },
                        selected = form.tag,
                        onSelected = onTagChange,
                    )
                    form.error?.let { error ->
                        Text(
                            stringResource(error.messageResource()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
                    ) {
                        OutlinedButton(onClick = onClearFilters) { Text(stringResource(R.string.clear_filters)) }
                        Button(onClick = onApplyFilters) { Text(stringResource(R.string.apply_filters)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDropdown(
    label: String,
    allLabel: String,
    choices: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected ?: allLabel,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            choices.distinct().forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice) },
                    onClick = {
                        onSelected(choice)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun ReportsFilterError.messageResource(): Int = when (this) {
    ReportsFilterError.INVALID_DATE -> R.string.report_invalid_date
    ReportsFilterError.INVALID_RANGE -> R.string.report_invalid_range
    ReportsFilterError.RANGE_TOO_LARGE -> R.string.report_range_too_large
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
private fun CashFlowChart(summary: ReportsSummary, currency: String) {
    val rows = summary.monthlyCashFlow
    val locale = LocalConfiguration.current.locales[0]
    val maximum = rows.maxOfOrNull { maxOf(it.income, it.expenses) } ?: BigDecimal.ZERO
    val incomeColor = MaterialTheme.colorScheme.creditAmount
    val expenseColor = MaterialTheme.colorScheme.debitAmount
    val rangeFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.report_range_date_pattern), locale)
    ReportCard(title = stringResource(R.string.report_cash_flow)) {
        Text(
            stringResource(
                R.string.report_range_value,
                summary.fromDate.format(rangeFormatter),
                summary.untilDate.format(rangeFormatter),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChartLegend(incomeColor, expenseColor)
        if (rows.size <= COMPACT_MONTH_COUNT) {
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
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(172.dp),
                horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
                verticalAlignment = Alignment.Bottom,
            ) {
                items(rows, key = MonthlyCashFlow::month) { row ->
                    MonthBars(
                        row = row,
                        maximum = maximum,
                        incomeColor = incomeColor,
                        expenseColor = expenseColor,
                        monthLabel = row.month.format(
                            DateTimeFormatter.ofPattern(stringResource(R.string.report_month_pattern), locale)
                        ),
                        modifier = Modifier.width(72.dp),
                    )
                }
            }
        }
        Text(
            stringResource(
                R.string.report_range_expense_total,
                formatCurrency(summary.expenses, currency),
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
private const val COMPACT_MONTH_COUNT = 4
