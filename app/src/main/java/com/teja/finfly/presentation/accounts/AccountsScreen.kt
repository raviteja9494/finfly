/* Presentation-layer Compose screen listing bank accounts and balances. */
package com.teja.finfly.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teja.finfly.R
import com.teja.finfly.domain.model.Account
import com.teja.finfly.presentation.components.EmptyState
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState
import com.teja.finfly.presentation.theme.FinFlyThemeTokens
import java.text.NumberFormat
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bank_accounts)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_bank_account)) },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.hasError -> ErrorState(modifier = Modifier.padding(padding))
            state.accounts.isEmpty() -> EmptyState(
                R.string.no_bank_accounts,
                R.string.no_bank_accounts_message,
                Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = FinFlyThemeTokens.spacing.medium,
                    end = FinFlyThemeTokens.spacing.medium,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + FinFlyThemeTokens.spacing.xLarge,
                ),
                verticalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.small),
            ) {
                items(state.accounts, key = { it.id }) { AccountCard(it) }
            }
        }
    }
}

@Composable
fun AccountCard(account: Account, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(FinFlyThemeTokens.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(FinFlyThemeTokens.spacing.medium),
        ) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(account.type, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatBalance(account), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatBalance(account: Account): String = NumberFormat.getCurrencyInstance().run {
    runCatching { currency = Currency.getInstance(account.currency) }
    format(account.balance)
}
