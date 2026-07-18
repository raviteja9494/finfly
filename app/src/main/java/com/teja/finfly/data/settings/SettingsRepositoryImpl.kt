/* Data-layer DataStore implementation of local Firefly settings persistence. */
package com.teja.finfly.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AppSettings
import com.teja.finfly.domain.model.DashboardChartPeriod
import com.teja.finfly.domain.model.DashboardRangeMode
import com.teja.finfly.domain.model.CategoryChartStyle
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope scope: CoroutineScope,
) : SettingsRepository {
    override val settings: StateFlow<AppSettings> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            AppSettings(
                serverUrl = preferences[SERVER_URL].orEmpty(),
                bearerToken = preferences[BEARER_TOKEN].orEmpty(),
                lastSyncTime = preferences[LAST_SYNC]?.let(Instant::ofEpochMilli),
                showNetWorthSummary = preferences[SHOW_NET_WORTH] ?: false,
                recentTransactionsCount = preferences[RECENT_TRANSACTION_COUNT]
                    ?.takeIf { it in SUPPORTED_RECENT_COUNTS } ?: DEFAULT_RECENT_COUNT,
                dashboardChartPeriod = preferences[DASHBOARD_CHART_PERIOD].toEnumOrDefault(
                    DashboardChartPeriod.WEEK,
                ),
                dashboardRangeMode = preferences[DASHBOARD_RANGE_MODE].toEnumOrDefault(
                    DashboardRangeMode.CALENDAR,
                ),
                showSpendingInsight = preferences[SHOW_SPENDING_INSIGHT] ?: true,
                categoryChartStyle = preferences[CATEGORY_CHART_STYLE].toEnumOrDefault(
                    CategoryChartStyle.BARS,
                ),
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    override suspend fun save(serverUrl: String, bearerToken: String): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            preferences[SERVER_URL] = serverUrl
            preferences[BEARER_TOKEN] = bearerToken
        }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: it.javaClass.simpleName, it) }

    override suspend fun updateLastSyncTime(instant: Instant): Result<Unit> = runCatching {
        dataStore.edit { it[LAST_SYNC] = instant.toEpochMilli() }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: it.javaClass.simpleName, it) }

    override suspend fun saveDashboardPreferences(
        showNetWorthSummary: Boolean,
        recentTransactionsCount: Int,
        chartPeriod: DashboardChartPeriod,
        rangeMode: DashboardRangeMode,
        showSpendingInsight: Boolean,
        categoryChartStyle: CategoryChartStyle,
    ): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            preferences[SHOW_NET_WORTH] = showNetWorthSummary
            preferences[RECENT_TRANSACTION_COUNT] = recentTransactionsCount
            preferences[DASHBOARD_CHART_PERIOD] = chartPeriod.name
            preferences[DASHBOARD_RANGE_MODE] = rangeMode.name
            preferences[SHOW_SPENDING_INSIGHT] = showSpendingInsight
            preferences[CATEGORY_CHART_STYLE] = categoryChartStyle.name
        }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: it.javaClass.simpleName, it) }

    private companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val BEARER_TOKEN = stringPreferencesKey("bearer_token")
        val LAST_SYNC = longPreferencesKey("last_sync_time")
        val SHOW_NET_WORTH = booleanPreferencesKey("show_net_worth_summary")
        val RECENT_TRANSACTION_COUNT = intPreferencesKey("recent_transaction_count")
        val DASHBOARD_CHART_PERIOD = stringPreferencesKey("dashboard_chart_period")
        val DASHBOARD_RANGE_MODE = stringPreferencesKey("dashboard_range_mode")
        val SHOW_SPENDING_INSIGHT = booleanPreferencesKey("show_spending_insight")
        val CATEGORY_CHART_STYLE = stringPreferencesKey("category_chart_style")
        const val DEFAULT_RECENT_COUNT = 10
        val SUPPORTED_RECENT_COUNTS = setOf(5, 10, 20)
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
}
