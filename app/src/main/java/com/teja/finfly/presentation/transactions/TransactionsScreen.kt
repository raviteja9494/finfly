/* Presentation-layer Compose screen for the compact, filterable transaction timeline. */
package com.teja.finfly.presentation.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.Category
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
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_transaction)) },
            )
        },
    ) { padding ->
        if (state.isLoading) LoadingState(Modifier.padding(padding))
        else TransactionList(
            state = state,
            onQueryChange = viewModel::setQuery,
            onTypeSelected = viewModel::setType,
            onCategorySelected = viewModel::setCategory,
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
    onQueryChange: (String) -> Unit,
    onTypeSelected: (TransactionType?) -> Unit,
    onCategorySelected: (String?) -> Unit,
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
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                CategoryDropdown(
                    categories = state.categories,
                    selected = state.filter.categories.firstOrNull(),
                    onSelected = onCategorySelected,
                    modifier = Modifier.weight(1f),
                )
                TypeDropdown(
                    selected = state.filter.types.firstOrNull(),
                    onSelected = onTypeSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.filter.query.isNotBlank() || state.filter.categories.isNotEmpty() || state.filter.types.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClearFilters) { Text(stringResource(R.string.clear_filters)) }
                }
            }
        }
        when {
            state.hasError -> item { ErrorState() }
            state.transactions.isEmpty() -> item {
                EmptyState(R.string.no_matching_transactions, R.string.adjust_filters)
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
private fun CategoryDropdown(
    categories: List<Category>,
    selected: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected ?: stringResource(R.string.all_categories), modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_categories)) },
                onClick = { expanded = false; onSelected(null) },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = { expanded = false; onSelected(category.name) },
                )
            }
        }
    }
}

@Composable
private fun TypeDropdown(
    selected: TransactionType?,
    onSelected: (TransactionType?) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.label() ?: stringResource(R.string.all_types), modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_types)) },
                onClick = { expanded = false; onSelected(null) },
            )
            TransactionType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label()) },
                    onClick = { expanded = false; onSelected(type) },
                )
            }
        }
    }
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
