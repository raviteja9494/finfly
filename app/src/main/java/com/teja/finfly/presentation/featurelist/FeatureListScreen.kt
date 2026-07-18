/* Presentation-layer Compose list for secondary Firefly drawer destinations. */
package com.teja.finfly.presentation.featurelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.FireflyFeature
import com.teja.finfly.domain.model.FireflyFeatureItem
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.theme.FinFlyThemeTokens

@Composable
fun FeatureListScreen(
    onAdd: (FireflyFeature) -> Unit,
    viewModel: FeatureListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
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
                is FeatureListUiState.Success -> FeatureItems(value.items)
            }
        }
    }
}

@Composable
private fun FeatureItems(items: List<FireflyFeatureItem>) {
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
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    if (item.details.isNotEmpty()) {
                        Text(
                            item.details.joinToString(stringResource(R.string.list_detail_separator)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
    FireflyFeature.BILLS -> R.string.no_bills_message
    FireflyFeature.PIGGY_BANKS -> R.string.no_piggy_banks_message
}

private fun FireflyFeature.addLabel(): Int = when (this) {
    FireflyFeature.BUDGETS -> R.string.add_budget
    FireflyFeature.CATEGORIES -> R.string.add_category
    FireflyFeature.BILLS -> R.string.add_bill
    FireflyFeature.PIGGY_BANKS -> R.string.add_piggy_bank
}
