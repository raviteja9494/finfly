/* Presentation-layer app scaffold combining type-safe navigation and persistent bottom tabs. */
package com.teja.finfly.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.teja.finfly.R
import com.teja.finfly.presentation.dashboard.DashboardScreen
import com.teja.finfly.presentation.reports.ReportsScreen
import com.teja.finfly.presentation.settings.SettingsScreen
import com.teja.finfly.presentation.transactions.TransactionsScreen

private enum class FinFlyTab(val label: Int, val icon: ImageVector) {
    DASHBOARD(R.string.nav_dashboard, Icons.Rounded.Dashboard),
    TRANSACTIONS(R.string.nav_transactions, Icons.Rounded.ReceiptLong),
    REPORTS(R.string.nav_reports, Icons.Rounded.Insights),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings),
}

@Composable
fun FinFlyApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FinFlyBottomBar(
                destination = backStackEntry?.destination,
                onSelect = { tab ->
                    val startId = navController.graph.findStartDestination().id
                    when (tab) {
                        FinFlyTab.DASHBOARD -> navController.navigate(AppRoute.Dashboard) {
                            popUpTo(startId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        FinFlyTab.TRANSACTIONS -> navController.navigate(AppRoute.Transactions) {
                            popUpTo(startId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        FinFlyTab.REPORTS -> navController.navigate(AppRoute.Reports) {
                            popUpTo(startId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        FinFlyTab.SETTINGS -> navController.navigate(AppRoute.Settings) {
                            popUpTo(startId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Dashboard,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<AppRoute.Dashboard> {
                DashboardScreen(onViewAll = { navController.navigate(AppRoute.Transactions) })
            }
            composable<AppRoute.Transactions> { TransactionsScreen() }
            composable<AppRoute.Reports> { ReportsScreen() }
            composable<AppRoute.Settings> { SettingsScreen() }
        }
    }
}

@Composable
private fun FinFlyBottomBar(destination: NavDestination?, onSelect: (FinFlyTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        FinFlyTab.entries.forEach { tab ->
            val selected = destination?.hierarchy?.any { item ->
                when (tab) {
                    FinFlyTab.DASHBOARD -> item.hasRoute<AppRoute.Dashboard>()
                    FinFlyTab.TRANSACTIONS -> item.hasRoute<AppRoute.Transactions>()
                    FinFlyTab.REPORTS -> item.hasRoute<AppRoute.Reports>()
                    FinFlyTab.SETTINGS -> item.hasRoute<AppRoute.Settings>()
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
