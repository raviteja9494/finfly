/* Presentation-layer app shell combining global drawer, sync app bar, tabs, and type-safe navigation. */
package com.teja.finfly.presentation.navigation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.teja.finfly.R
import com.teja.finfly.domain.model.DailySpend
import com.teja.finfly.domain.model.FireflyFeature
import com.teja.finfly.domain.model.SyncState
import com.teja.finfly.presentation.accounts.AccountEditorScreen
import com.teja.finfly.presentation.accounts.AccountsScreen
import com.teja.finfly.presentation.dashboard.DashboardScreen
import com.teja.finfly.presentation.featurelist.FeatureListScreen
import com.teja.finfly.presentation.featureeditor.FeatureEditorScreen
import com.teja.finfly.presentation.assistant.AssistantScreen
import com.teja.finfly.presentation.reports.ReportsScreen
import com.teja.finfly.presentation.settings.SettingsScreen
import com.teja.finfly.presentation.transactiondetail.TransactionDetailScreen
import com.teja.finfly.presentation.transactioneditor.TransactionEditorScreen
import com.teja.finfly.presentation.transactions.TransactionsScreen
import com.teja.finfly.presentation.smsrules.SmsRulesScreen
import com.teja.finfly.presentation.smsrules.BankRuleEditorScreen
import com.teja.finfly.presentation.smsrules.CategoryRuleEditorScreen
import com.teja.finfly.presentation.smsrules.SmsLogsScreen
import com.teja.finfly.presentation.smsrules.SmsLogDetailScreen
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import androidx.compose.runtime.CompositionLocalProvider
import com.teja.finfly.presentation.theme.LocalFinFlyZoneId

private enum class FinFlyTab(val label: Int, val icon: ImageVector) {
    DASHBOARD(R.string.nav_dashboard, Icons.Rounded.Dashboard),
    TRANSACTIONS(R.string.nav_transactions, Icons.Rounded.ReceiptLong),
    REPORTS(R.string.nav_reports, Icons.Rounded.Insights),
    ASSISTANT(R.string.nav_assistant, Icons.Rounded.AutoAwesome),
}

private data class DrawerDestination(
    val label: Int,
    val icon: ImageVector,
    val route: AppRoute,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinFlyApp(viewModel: AppShellViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val shellState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerDestinations = drawerDestinations()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FinFlyDrawer(
                shellState = shellState,
                destinations = drawerDestinations,
                currentEntry = backStackEntry,
                onNavigate = { route ->
                    navController.navigateDrawerRoute(route)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                FinFlyTopBar(
                    title = destinationTitle(backStackEntry),
                    syncState = (shellState as? AppShellUiState.Ready)?.syncState ?: SyncState.Idle,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSync = viewModel::sync,
                )
            },
            bottomBar = {
                if (backStackEntry.isTopLevelDestination()) {
                    FinFlyBottomBar(
                        destination = backStackEntry?.destination,
                        onSelect = { tab -> navController.navigateTab(tab) },
                    )
                }
            },
        ) { innerPadding ->
            val useDeviceZone = (shellState as? AppShellUiState.Ready)?.useDeviceTimezone ?: true
            CompositionLocalProvider(
                LocalFinFlyZoneId provides if (useDeviceZone) ZoneId.systemDefault() else ZoneOffset.UTC,
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Dashboard,
                    modifier = Modifier.padding(innerPadding),
                ) {
                composable<AppRoute.Dashboard> {
                    DashboardScreen(
                        onViewAll = { navController.navigate(AppRoute.Transactions()) },
                        onTransactionClick = { navController.navigate(AppRoute.TransactionDetail(it)) },
                        onDaySelected = { navController.navigate(it.toTransactionRoute()) },
                    )
                }
                composable<AppRoute.Transactions> {
                    TransactionsScreen(
                        onAddTransaction = { navController.navigate(AppRoute.TransactionEditor()) },
                        onTransactionClick = { navController.navigate(AppRoute.TransactionDetail(it)) },
                    )
                }
                composable<AppRoute.Reports> { ReportsScreen() }
                composable<AppRoute.Settings> { SettingsScreen() }
                composable<AppRoute.Assistant> { AssistantScreen() }
                composable<AppRoute.TransactionDetail> {
                    TransactionDetailScreen(
                        onBack = navController::popBackStack,
                        onEdit = { navController.navigate(AppRoute.TransactionEditor(it)) },
                    )
                }
                composable<AppRoute.TransactionEditor> {
                    TransactionEditorScreen(onBack = navController::popBackStack)
                }
                composable<AppRoute.Accounts> {
                    AccountsScreen(
                        onAdd = { navController.navigate(AppRoute.AccountEditor()) },
                        onAccountClick = { navController.navigate(AppRoute.AccountEditor(it)) },
                        onViewTransactions = { navController.navigate(AppRoute.Transactions(accountId = it)) },
                    )
                }
                composable<AppRoute.AccountEditor> {
                    AccountEditorScreen(onBack = navController::popBackStack)
                }
                composable<AppRoute.FeatureList> {
                    FeatureListScreen(
                        onAdd = { navController.navigate(AppRoute.FeatureEditor(it)) },
                        onEdit = { feature, id -> navController.navigate(AppRoute.FeatureEditor(feature, id)) },
                    )
                }
                composable<AppRoute.FeatureEditor> {
                    FeatureEditorScreen(onBack = navController::popBackStack)
                }
                composable<AppRoute.SmsParsing> {
                    SmsRulesScreen(
                        onAddBankRule = { navController.navigate(AppRoute.BankRuleEditor()) },
                        onEditBankRule = { navController.navigate(AppRoute.BankRuleEditor(it)) },
                        onAddCategoryRule = { navController.navigate(AppRoute.CategoryRuleEditor()) },
                        onEditCategoryRule = { navController.navigate(AppRoute.CategoryRuleEditor(it)) },
                        onOpenLogs = { navController.navigate(AppRoute.SmsLogs) },
                    )
                }
                composable<AppRoute.BankRuleEditor> {
                    BankRuleEditorScreen(onBack = navController::popBackStack)
                }
                composable<AppRoute.CategoryRuleEditor> {
                    CategoryRuleEditorScreen(onBack = navController::popBackStack)
                }
                composable<AppRoute.SmsLogs> {
                    SmsLogsScreen(onLogClick = { navController.navigate(AppRoute.SmsLogDetail(it)) })
                }
                composable<AppRoute.SmsLogDetail> {
                    SmsLogDetailScreen(
                        onBack = navController::popBackStack,
                        onCreateRule = { navController.navigate(AppRoute.BankRuleEditor(prefillSender = it)) },
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun drawerDestinations(): List<DrawerDestination> = listOf(
    DrawerDestination(R.string.drawer_accounts, Icons.Rounded.AccountBalance, AppRoute.Accounts),
    DrawerDestination(
        R.string.drawer_budgets,
        Icons.Rounded.AccountBalanceWallet,
        AppRoute.FeatureList(FireflyFeature.BUDGETS),
    ),
    DrawerDestination(
        R.string.drawer_categories,
        Icons.Rounded.Category,
        AppRoute.FeatureList(FireflyFeature.CATEGORIES),
    ),
    DrawerDestination(
        R.string.drawer_tags,
        Icons.Rounded.Label,
        AppRoute.FeatureList(FireflyFeature.TAGS),
    ),
    DrawerDestination(
        R.string.drawer_bills,
        Icons.Rounded.ReceiptLong,
        AppRoute.FeatureList(FireflyFeature.BILLS),
    ),
    DrawerDestination(
        R.string.drawer_piggy_banks,
        Icons.Rounded.Savings,
        AppRoute.FeatureList(FireflyFeature.PIGGY_BANKS),
    ),
    DrawerDestination(
        R.string.drawer_firefly_rules,
        Icons.Rounded.Rule,
        AppRoute.FeatureList(FireflyFeature.RULES),
    ),
    DrawerDestination(R.string.drawer_sms_parsing, Icons.Rounded.Sms, AppRoute.SmsParsing),
    DrawerDestination(R.string.nav_settings, Icons.Rounded.Settings, AppRoute.Settings),
)

@Composable
private fun FinFlyDrawer(
    shellState: AppShellUiState,
    destinations: List<DrawerDestination>,
    currentEntry: NavBackStackEntry?,
    onNavigate: (AppRoute) -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Icon(Icons.Rounded.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            val ready = shellState as? AppShellUiState.Ready
            Text(
                ready?.serverUrl?.takeIf(String::isNotBlank) ?: stringResource(R.string.server_not_connected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                lastSyncText(ready?.lastSyncTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        destinations.forEach { destination ->
            NavigationDrawerItem(
                label = { Text(stringResource(destination.label)) },
                selected = currentEntry.matches(destination.route),
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinFlyTopBar(
    title: String,
    syncState: SyncState,
    onOpenDrawer: () -> Unit,
    onSync: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.open_navigation_drawer))
            }
        },
        actions = {
            IconButton(onClick = onSync, enabled = syncState !is SyncState.Syncing) {
                SyncIcon(syncState)
            }
        },
    )
}

@Composable
private fun SyncIcon(state: SyncState) {
    when (state) {
        SyncState.Idle -> Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.sync_now))
        is SyncState.Syncing -> {
            val transition = rememberInfiniteTransition(label = "sync")
            val rotation by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
                label = "sync_rotation",
            )
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.syncing),
                modifier = Modifier.rotate(rotation),
            )
        }
        is SyncState.Success -> Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = stringResource(R.string.sync_complete),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        is SyncState.Error -> Icon(
            Icons.Rounded.Error,
            contentDescription = stringResource(R.string.sync_failed),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun lastSyncText(lastSync: Instant?): String {
    if (lastSync == null) return stringResource(R.string.last_sync_never)
    val minutes = Duration.between(lastSync, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.last_synced_just_now)
        minutes < 60 -> stringResource(R.string.last_synced_minutes, minutes)
        minutes < 1_440 -> stringResource(R.string.last_synced_hours, minutes / 60)
        else -> stringResource(R.string.last_synced_days, minutes / 1_440)
    }
}

@Composable
private fun destinationTitle(entry: NavBackStackEntry?): String {
    val destination = entry?.destination
    val resource = when {
        destination?.hasRoute<AppRoute.Dashboard>() == true -> R.string.nav_dashboard
        destination?.hasRoute<AppRoute.Transactions>() == true -> R.string.nav_transactions
        destination?.hasRoute<AppRoute.Reports>() == true -> R.string.nav_reports
        destination?.hasRoute<AppRoute.Settings>() == true -> R.string.nav_settings
        destination?.hasRoute<AppRoute.Assistant>() == true -> R.string.nav_assistant
        destination?.hasRoute<AppRoute.TransactionDetail>() == true -> R.string.transaction_details
        destination?.hasRoute<AppRoute.TransactionEditor>() == true -> R.string.edit_transaction
        destination?.hasRoute<AppRoute.Accounts>() == true -> R.string.drawer_accounts
        destination?.hasRoute<AppRoute.AccountEditor>() == true ->
            if (entry?.toRoute<AppRoute.AccountEditor>()?.accountId == null) R.string.new_bank_account
            else R.string.edit_bank_account
        destination?.hasRoute<AppRoute.FeatureList>() == true ->
            entry?.toRoute<AppRoute.FeatureList>()?.feature?.titleResource() ?: R.string.app_name
        destination?.hasRoute<AppRoute.FeatureEditor>() == true -> entry?.toRoute<AppRoute.FeatureEditor>()?.let { route ->
            if (route.itemId == null) route.feature.createTitleResource() else route.feature.titleResource()
        } ?: R.string.app_name
        destination?.hasRoute<AppRoute.SmsParsing>() == true -> R.string.drawer_sms_parsing
        destination?.hasRoute<AppRoute.BankRuleEditor>() == true -> R.string.edit_bank_rule
        destination?.hasRoute<AppRoute.CategoryRuleEditor>() == true -> R.string.edit_category_rule
        destination?.hasRoute<AppRoute.SmsLogs>() == true -> R.string.sms_log
        destination?.hasRoute<AppRoute.SmsLogDetail>() == true -> R.string.sms_log_details
        else -> R.string.app_name
    }
    return stringResource(resource)
}

private fun FireflyFeature.titleResource(): Int = when (this) {
    FireflyFeature.BUDGETS -> R.string.drawer_budgets
    FireflyFeature.CATEGORIES -> R.string.drawer_categories
    FireflyFeature.TAGS -> R.string.drawer_tags
    FireflyFeature.BILLS -> R.string.drawer_bills
    FireflyFeature.PIGGY_BANKS -> R.string.drawer_piggy_banks
    FireflyFeature.RULES -> R.string.drawer_firefly_rules
}

private fun NavBackStackEntry?.isTopLevelDestination(): Boolean {
    val destination = this?.destination ?: return false
    if (destination.hasRoute<AppRoute.Transactions>()) {
        val route = runCatching { this?.toRoute<AppRoute.Transactions>() }.getOrNull()
        return route == AppRoute.Transactions()
    }
    return destination.hasRoute<AppRoute.Dashboard>() || destination.hasRoute<AppRoute.Reports>() ||
        destination.hasRoute<AppRoute.Assistant>()
}

private fun NavBackStackEntry?.matches(route: AppRoute): Boolean = when (route) {
    AppRoute.Accounts -> this?.destination?.hasRoute<AppRoute.Accounts>() == true
    AppRoute.Settings -> this?.destination?.hasRoute<AppRoute.Settings>() == true
    AppRoute.SmsParsing -> this?.destination?.let { destination ->
        destination.hasRoute<AppRoute.SmsParsing>() ||
            destination.hasRoute<AppRoute.BankRuleEditor>() ||
            destination.hasRoute<AppRoute.CategoryRuleEditor>() ||
            destination.hasRoute<AppRoute.SmsLogs>() ||
            destination.hasRoute<AppRoute.SmsLogDetail>()
    } == true
    is AppRoute.FeatureList -> when {
        this?.destination?.hasRoute<AppRoute.FeatureList>() == true ->
            runCatching { this.toRoute<AppRoute.FeatureList>().feature == route.feature }.getOrDefault(false)
        this?.destination?.hasRoute<AppRoute.FeatureEditor>() == true ->
            runCatching { this.toRoute<AppRoute.FeatureEditor>().feature == route.feature }.getOrDefault(false)
        else -> false
    }
    else -> false
}

private fun NavHostController.navigateTab(tab: FinFlyTab) {
    val startId = graph.findStartDestination().id
    when (tab) {
        FinFlyTab.DASHBOARD -> navigate(AppRoute.Dashboard) {
            configureTabNavigation(startId)
        }
        FinFlyTab.TRANSACTIONS -> navigate(AppRoute.Transactions()) {
            configureTabNavigation(startId)
        }
        FinFlyTab.REPORTS -> navigate(AppRoute.Reports) {
            configureTabNavigation(startId)
        }
        FinFlyTab.ASSISTANT -> navigate(AppRoute.Assistant) {
            configureTabNavigation(startId)
        }
    }
}

private fun androidx.navigation.NavOptionsBuilder.configureTabNavigation(startId: Int) {
    popUpTo(startId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private fun NavHostController.navigateDrawerRoute(route: AppRoute) {
    when (route) {
        AppRoute.Accounts,
        AppRoute.Settings,
        AppRoute.SmsParsing,
        is AppRoute.FeatureList -> navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        else -> Unit
    }
}

@Composable
private fun FinFlyBottomBar(destination: NavDestination?, onSelect: (FinFlyTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        FinFlyTab.entries.forEach { tab ->
            val selected = destination?.hierarchy?.any { item ->
                when (tab) {
                    FinFlyTab.DASHBOARD -> item.hasRoute<AppRoute.Dashboard>()
                    FinFlyTab.TRANSACTIONS -> item.hasRoute<AppRoute.Transactions>()
                    FinFlyTab.REPORTS -> item.hasRoute<AppRoute.Reports>()
                    FinFlyTab.ASSISTANT -> item.hasRoute<AppRoute.Assistant>()
                }
            } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

private fun FireflyFeature.createTitleResource(): Int = when (this) {
    FireflyFeature.BUDGETS -> R.string.new_budget
    FireflyFeature.CATEGORIES -> R.string.new_category
    FireflyFeature.TAGS -> R.string.new_tag
    FireflyFeature.BILLS -> R.string.new_bill
    FireflyFeature.PIGGY_BANKS -> R.string.new_piggy_bank
    FireflyFeature.RULES -> R.string.new_firefly_rule
}

private fun DailySpend.toTransactionRoute(): AppRoute.Transactions {
    val zone = ZoneId.systemDefault()
    return AppRoute.Transactions(
        fromEpochMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
        untilEpochMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
