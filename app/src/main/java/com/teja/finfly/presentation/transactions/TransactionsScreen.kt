/* Presentation-layer Compose screen for the paginated Room transaction timeline. */
package com.teja.finfly.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.components.TransactionRow
import com.teja.finfly.presentation.theme.FinFlyThemeTokens

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val value = state) {
        TransactionsUiState.Loading -> LoadingState()
        TransactionsUiState.Empty -> EmptyState(R.string.no_transactions, R.string.no_transactions_message)
        TransactionsUiState.Error -> ErrorState()
        is TransactionsUiState.Success -> TransactionList(value, viewModel::loadMore)
    }
}

@Composable
private fun TransactionList(state: TransactionsUiState.Success, onLoadMore: () -> Unit) {
    val spacing = FinFlyThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item {
            Column(Modifier.padding(bottom = spacing.medium)) {
                Text(stringResource(R.string.transactions_title), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.transactions_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(state.transactions, key = { _, item -> item.id }) { index, transaction ->
            TransactionRow(transaction)
            if (index >= state.transactions.lastIndex - LOAD_MORE_THRESHOLD) {
                LaunchedEffect(state.transactions.size) { onLoadMore() }
            }
        }
        if (state.isLoadingMore) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 4
