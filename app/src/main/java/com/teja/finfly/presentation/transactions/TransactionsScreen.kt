/* Presentation-layer Compose screen for a searchable, filterable transaction timeline. */
package com.teja.finfly.presentation.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.components.TransactionRow
import com.teja.finfly.presentation.theme.FinFlyThemeTokens

@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_transaction)) },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        TransactionList(
            state = state,
            showFilters = showFilters,
            onToggleFilters = { showFilters = !showFilters },
            onQueryChange = viewModel::setQuery,
            onToggleType = viewModel::toggleType,
            onToggleCategory = viewModel::toggleCategory,
            onToggleTag = viewModel::toggleTag,
            onToggleAccount = viewModel::toggleAccount,
            onClearFilters = viewModel::clearFilters,
            onLoadMore = viewModel::loadMore,
            onTransactionClick = onTransactionClick,
            contentPadding = padding,
        )
    }
}

@Composable
private fun TransactionList(
    state: TransactionsUiState,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleType: (TransactionType) -> Unit,
    onToggleCategory: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleAccount: (String) -> Unit,
    onClearFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onTransactionClick: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = FinFlyThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.medium,
            end = spacing.medium,
            top = spacing.medium + contentPadding.calculateTopPadding(),
            bottom = spacing.xLarge + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item {
            Column(Modifier.padding(bottom = spacing.small)) {
                Text(stringResource(R.string.transactions_title), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.transactions_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.search_transactions)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onToggleFilters) {
                        Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.filters))
                    }
                },
            )
        }
        if (state.filter.isActive) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.filters_active), style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = onClearFilters) { Text(stringResource(R.string.clear_filters)) }
                }
            }
        }
        if (showFilters) {
            item { FilterLabel(R.string.transaction_type) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    items(TransactionType.entries) { type ->
                        FilterChip(
                            selected = type in state.filter.types,
                            onClick = { onToggleType(type) },
                            label = { Text(type.label()) },
                        )
                    }
                }
            }
            if (state.categories.isNotEmpty()) {
                item { FilterLabel(R.string.categories) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        items(state.categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = category.name in state.filter.categories,
                                onClick = { onToggleCategory(category.name) },
                                label = { Text(category.name) },
                            )
                        }
                    }
                }
            }
            if (state.tags.isNotEmpty()) {
                item { FilterLabel(R.string.tags) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        items(state.tags, key = { it.id }) { tag ->
                            FilterChip(
                                selected = tag.name in state.filter.tags,
                                onClick = { onToggleTag(tag.name) },
                                label = { Text(tag.name) },
                            )
                        }
                    }
                }
            }
            if (state.accounts.isNotEmpty()) {
                item { FilterLabel(R.string.bank_accounts) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        items(state.accounts, key = { it.id }) { account ->
                            FilterChip(
                                selected = account.id in state.filter.accountIds,
                                onClick = { onToggleAccount(account.id) },
                                label = { Text(account.name) },
                            )
                        }
                    }
                }
            }
        }
        when {
            state.hasError -> item { ErrorState() }
            state.transactions.isEmpty() -> item {
                EmptyState(
                    if (state.filter.isActive) R.string.no_matching_transactions else R.string.no_transactions,
                    if (state.filter.isActive) R.string.adjust_filters else R.string.no_transactions_message,
                )
            }
            else -> itemsIndexed(state.transactions, key = { _, item -> item.id }) { index, transaction ->
                TransactionRow(
                    transaction = transaction,
                    modifier = Modifier.clickable { onTransactionClick(transaction.id) },
                )
                if (index >= state.transactions.lastIndex - LOAD_MORE_THRESHOLD) {
                    LaunchedEffect(state.transactions.size) { onLoadMore() }
                }
            }
        }
        if (state.hasMore) item { CircularProgressIndicator(modifier = Modifier.padding(spacing.large)) }
    }
}

@Composable
private fun FilterLabel(resource: Int) {
    Text(stringResource(resource), style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun TransactionType.label(): String = stringResource(
    when (this) {
        TransactionType.WITHDRAWAL -> R.string.transaction_withdrawal
        TransactionType.DEPOSIT -> R.string.transaction_deposit
        TransactionType.TRANSFER -> R.string.transaction_transfer
    }
)

private const val LOAD_MORE_THRESHOLD = 4
