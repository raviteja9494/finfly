/* Presentation-layer Compose screen for the compact, filterable transaction timeline. */
package com.teja.finflyiii.presentation.transactions

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finflyiii.R
import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.TransactionType
import com.teja.finflyiii.domain.model.Tag
import com.teja.finflyiii.presentation.components.EmptyState
import com.teja.finflyiii.presentation.components.ErrorState
import com.teja.finflyiii.presentation.components.LoadingState
import com.teja.finflyiii.presentation.components.TransactionRow
import com.teja.finflyiii.presentation.theme.FinFlyIIIThemeTokens
import kotlinx.coroutines.flow.distinctUntilChanged

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
            onTagSelected = viewModel::setTag,
            onClearFilters = viewModel::clearFilters,
            onRetry = viewModel::retry,
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
    onTagSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onTransactionClick: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = FinFlyIIIThemeTokens.spacing
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var showSearch by rememberSaveable { mutableStateOf(state.searchQuery.isNotEmpty()) }
    var showFilters by rememberSaveable { mutableStateOf(state.activeFilterCount > 0) }
    LaunchedEffect(showSearch) {
        if (showSearch) searchFocusRequester.requestFocus()
    }
    LaunchedEffect(listState, state.transactions.size, state.hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (state.hasMore && lastVisibleIndex >= state.transactions.lastIndex - LOAD_MORE_THRESHOLD) {
                    onLoadMore()
                }
            }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()),
    ) {
        Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 2.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xSmall),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(
                            R.string.transactions_loaded_count,
                            state.transactions.size,
                            state.resultCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.search_transactions),
                            tint = if (showSearch || state.searchQuery.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        BadgedBox(
                            badge = {
                                if (state.activeFilterCount > 0) {
                                    Badge { Text(state.activeFilterCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.FilterList,
                                contentDescription = stringResource(R.string.filters),
                                tint = if (showFilters || state.activeFilterCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (showSearch) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                        singleLine = true,
                        label = { Text(stringResource(R.string.search_transactions)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (state.searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                                }
                            }
                        } else null,
                    )
                }
                if (showFilters) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (state.activeFilterCount > 0) {
                                stringResource(R.string.filters_count, state.activeFilterCount)
                            } else stringResource(R.string.filters),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.activeFilterCount > 0) {
                            TextButton(onClick = onClearFilters) { Text(stringResource(R.string.clear_filters)) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = spacing.small),
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
                        TagDropdown(
                            tags = state.tags,
                            selected = state.filter.tags.firstOrNull(),
                            onSelected = onTagSelected,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = spacing.medium,
                end = spacing.medium,
                top = spacing.medium,
                bottom = spacing.xLarge + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            when {
                state.hasError -> item { ErrorState(onRetry = onRetry) }
                state.transactions.isEmpty() -> item {
                    EmptyState(R.string.no_matching_transactions, R.string.adjust_filters)
                }
                else -> itemsIndexed(state.transactions, key = { _, item -> item.id }) { _, transaction ->
                    TransactionRow(
                        transaction = transaction,
                        modifier = Modifier.clickable { onTransactionClick(transaction.id) },
                    )
                }
            }
            if (state.hasMore) item {
                OutlinedButton(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small),
                ) {
                    Text(stringResource(R.string.load_more_transactions))
                }
            }
        }
    }
}

@Composable
private fun TagDropdown(
    tags: List<Tag>,
    selected: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected ?: stringResource(R.string.all_tags), modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_tags)) },
                onClick = { expanded = false; onSelected(null) },
            )
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(tag.name) },
                    onClick = { expanded = false; onSelected(tag.name) },
                )
            }
        }
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

private const val LOAD_MORE_THRESHOLD = 8
