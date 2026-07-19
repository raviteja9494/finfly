/* Presentation-layer Compose list for secondary Firefly drawer destinations. */
package com.teja.finfly.presentation.featurelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.teja.finfly.R
import com.teja.finfly.domain.model.FireflyFeature
import com.teja.finfly.domain.model.FireflyFeatureItem
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.components.ConfirmationDialog
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

@Composable
fun FeatureListScreen(
    onAdd: (FireflyFeature) -> Unit,
    onEdit: (FireflyFeature, String) -> Unit,
    viewModel: FeatureListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deletionState by viewModel.deletionState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<FireflyFeatureItem?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    pendingDelete?.let { item ->
        ConfirmationDialog(
            title = R.string.delete_item,
            message = stringResource(R.string.delete_item_message, item.title),
            confirmLabel = R.string.delete,
            onConfirm = {
                pendingDelete = null
                viewModel.delete(item.id)
            },
            onDismiss = { pendingDelete = null },
            destructive = true,
        )
    }
    if (deletionState.failed) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteError,
            title = { Text(stringResource(R.string.delete_failed)) },
            text = { Text(stringResource(R.string.delete_failed_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteError) { Text(stringResource(R.string.ok)) }
            },
        )
    }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(viewModel.feature) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(viewModel.feature.addLabel())) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val value = state) {
                FeatureListUiState.Loading -> LoadingState()
                FeatureListUiState.Error -> ErrorState(onRetry = viewModel::load)
                FeatureListUiState.Empty -> EmptyState(
                    title = R.string.no_items,
                    message = viewModel.feature.emptyMessage(),
                )
                is FeatureListUiState.Success -> FeatureItems(
                    viewModel.feature,
                    value.items,
                    deletionState.deletingId,
                    onEdit = { onEdit(viewModel.feature, it.id) },
                    onDelete = { pendingDelete = it },
                )
            }
        }
    }
}

@Composable
private fun FeatureItems(
    feature: FireflyFeature,
    items: List<FireflyFeatureItem>,
    deletingId: String?,
    onEdit: (FireflyFeatureItem) -> Unit,
    onDelete: (FireflyFeatureItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(FinFlyThemeTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
    ) {
        items(items, key = FireflyFeatureItem::id) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(FinFlyThemeTokens.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onEdit(item) }, enabled = deletingId == null) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_item_named, item.title))
                        }
                        IconButton(onClick = { onDelete(item) }, enabled = deletingId == null) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.delete_item_named, item.title),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (item.details.isNotEmpty()) {
                        Text(
                            item.details.joinToString(stringResource(R.string.list_detail_separator)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (feature == FireflyFeature.BUDGETS) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.budget_set_value, formatFeatureAmount(item.setAmount, item.currencyCode)))
                            Text(
                                stringResource(R.string.budget_spent_value, formatFeatureAmount(item.spentAmount, item.currencyCode)),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    item.progressPercent?.let { percentage ->
                        LinearProgressIndicator(
                            progress = { percentage.coerceIn(0, 100) / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(R.string.percent_complete, percentage),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun FireflyFeature.emptyMessage(): Int = when (this) {
    FireflyFeature.BUDGETS -> R.string.no_budgets_message
    FireflyFeature.CATEGORIES -> R.string.no_categories_message
    FireflyFeature.TAGS -> R.string.no_tags_message
    FireflyFeature.BILLS -> R.string.no_bills_message
    FireflyFeature.PIGGY_BANKS -> R.string.no_piggy_banks_message
    FireflyFeature.RULES -> R.string.no_firefly_rules_message
}

private fun FireflyFeature.addLabel(): Int = when (this) {
    FireflyFeature.BUDGETS -> R.string.add_budget
    FireflyFeature.CATEGORIES -> R.string.add_category
    FireflyFeature.TAGS -> R.string.add_tag
    FireflyFeature.BILLS -> R.string.add_bill
    FireflyFeature.PIGGY_BANKS -> R.string.add_piggy_bank
    FireflyFeature.RULES -> R.string.add_firefly_rule
}

@Composable
private fun formatFeatureAmount(amount: BigDecimal?, currency: String): String {
    if (amount == null) return stringResource(R.string.amount_not_set)
    return NumberFormat.getCurrencyInstance().run {
        runCatching { this.currency = Currency.getInstance(currency) }
        format(amount)
    }
}
