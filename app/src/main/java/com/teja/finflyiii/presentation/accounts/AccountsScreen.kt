/* Presentation-layer Compose screen grouping every Firefly account family. */
package com.teja.finflyiii.presentation.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finflyiii.R
import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.AccountGroup
import com.teja.finflyiii.presentation.components.EmptyState
import com.teja.finflyiii.presentation.components.ErrorState
import com.teja.finflyiii.presentation.components.LoadingState
import com.teja.finflyiii.presentation.components.ConfirmationDialog
import com.teja.finflyiii.presentation.theme.FinFlyIIIThemeTokens
import com.teja.finflyiii.presentation.theme.creditAmount
import com.teja.finflyiii.presentation.theme.debitAmount
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onAdd: () -> Unit,
    onAccountClick: (String) -> Unit,
    onViewTransactions: (String) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deletionState by viewModel.deletionState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Account?>(null) }
    val refreshing = (state as? AccountsUiState.Success)?.isRefreshing
        ?: (state as? AccountsUiState.Empty)?.isRefreshing
        ?: false
    pendingDelete?.let { account ->
        ConfirmationDialog(
            title = R.string.delete_account,
            message = stringResource(R.string.delete_account_message, account.name),
            confirmLabel = R.string.delete,
            onConfirm = {
                pendingDelete = null
                viewModel.delete(account.id)
            },
            onDismiss = { pendingDelete = null },
            destructive = true,
        )
    }
    if (deletionState.failed) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteError,
            title = { Text(stringResource(R.string.delete_failed)) },
            text = { Text(stringResource(R.string.delete_account_failed_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteError) { Text(stringResource(R.string.ok)) }
            },
        )
    }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_bank_account)) },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (val value = state) {
                AccountsUiState.Loading -> LoadingState()
                AccountsUiState.Error -> ErrorState(onRetry = viewModel::refresh)
                is AccountsUiState.Empty -> EmptyState(R.string.no_bank_accounts, R.string.no_bank_accounts_message)
                is AccountsUiState.Success -> GroupedAccountList(
                    value.accounts,
                    onAccountClick,
                    onViewTransactions,
                    deletionState.deletingId,
                    onDelete = { pendingDelete = it },
                )
            }
        }
    }
}

@Composable
private fun GroupedAccountList(
    accounts: List<Account>,
    onAccountClick: (String) -> Unit,
    onViewTransactions: (String) -> Unit,
    deletingId: String?,
    onDelete: (Account) -> Unit,
) {
    val grouped = AccountGroup.entries.mapNotNull { group ->
        val rows = accounts.filter { it.group == group }
        if (rows.isNotEmpty()) group to rows else null
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(FinFlyIIIThemeTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.small),
    ) {
        grouped.forEach { (group, rows) ->
            item(key = "header-${group.name}") {
                Text(
                    stringResource(group.labelResource()),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = FinFlyIIIThemeTokens.spacing.medium),
                )
            }
            items(rows, key = Account::id) { account ->
                AccountCard(
                    account = account,
                    modifier = Modifier.clickable { onAccountClick(account.id) },
                    onViewTransactions = { onViewTransactions(account.id) },
                    deleting = deletingId == account.id,
                    onDelete = { onDelete(account) },
                )
            }
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    modifier: Modifier = Modifier,
    deleting: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onViewTransactions: (() -> Unit)? = null,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(FinFlyIIIThemeTokens.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.medium),
        ) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(FinFlyIIIThemeTokens.spacing.small)) {
                    Text(account.currency, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            stringResource(account.group.labelResource()),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                formatBalance(account),
                style = MaterialTheme.typography.titleMedium,
                color = if (account.balance < BigDecimal.ZERO) MaterialTheme.colorScheme.debitAmount
                else MaterialTheme.colorScheme.creditAmount,
            )
            if (onViewTransactions != null) {
                IconButton(onClick = onViewTransactions) {
                    Icon(
                        Icons.Rounded.ReceiptLong,
                        contentDescription = stringResource(R.string.view_account_transactions),
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, enabled = !deleting) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.delete_item_named, account.name),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun AccountGroup.labelResource(): Int = when (this) {
    AccountGroup.ASSET -> R.string.account_group_asset
    AccountGroup.LIABILITY -> R.string.account_group_liability
    AccountGroup.REVENUE -> R.string.account_group_revenue
    AccountGroup.EXPENSE -> R.string.account_group_expense
}

private fun formatBalance(account: Account): String = NumberFormat.getCurrencyInstance().run {
    runCatching { currency = Currency.getInstance(account.currency) }
    format(account.balance)
}
