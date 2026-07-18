/* Presentation-layer Compose screen grouping every Firefly account family. */
package com.teja.finfly.presentation.accounts

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
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.AccountGroup
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import com.teja.finfly.presentation.theme.creditAmount
import com.teja.finfly.presentation.theme.debitAmount
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onAdd: () -> Unit,
    onAccountClick: (String) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshing = (state as? AccountsUiState.Success)?.isRefreshing
        ?: (state as? AccountsUiState.Empty)?.isRefreshing
        ?: false
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
                is AccountsUiState.Success -> GroupedAccountList(value.accounts, onAccountClick)
            }
        }
    }
}

@Composable
private fun GroupedAccountList(accounts: List<Account>, onAccountClick: (String) -> Unit) {
    val grouped = AccountGroup.entries.mapNotNull { group ->
        val rows = accounts.filter { it.group == group }
        if (rows.isNotEmpty()) group to rows else null
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(FinFlyThemeTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
    ) {
        grouped.forEach { (group, rows) ->
            item(key = "header-${group.name}") {
                Text(
                    stringResource(group.labelResource()),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = FinFlyThemeTokens.spacing.medium),
                )
            }
            items(rows, key = Account::id) { account ->
                AccountCard(
                    account = account,
                    modifier = Modifier.clickable { onAccountClick(account.id) },
                )
            }
        }
    }
}

@Composable
fun AccountCard(account: Account, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(FinFlyThemeTokens.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
        ) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small)) {
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
