/* Unit tests for domain validation and normalization of Firefly settings. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AppSettings
import com.teja.finfly.domain.model.DashboardChartPeriod
import com.teja.finfly.domain.model.DashboardRangeMode
import com.teja.finfly.domain.model.CategoryChartStyle
import com.teja.finfly.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SaveSettingsUseCaseTest {
    private val repository = FakeSettingsRepository()
    private val useCase = SaveSettingsUseCase(repository)

    @Test
    fun `normalizes valid server URL`() = runBlocking {
        val result = useCase("https://firefly.example.test", " secret ")
        assertTrue(result is Result.Success)
        assertEquals("https://firefly.example.test/", repository.settings.value.serverUrl)
        assertEquals("secret", repository.settings.value.bearerToken)
    }

    @Test
    fun `rejects non-http URL`() = runBlocking {
        val result = useCase("ftp://firefly.example.test", "secret")
        assertEquals(Result.Error("invalid_url"), result)
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val mutableSettings = MutableStateFlow(AppSettings())
        override val settings: StateFlow<AppSettings> = mutableSettings

        override suspend fun save(serverUrl: String, bearerToken: String): Result<Unit> {
            mutableSettings.value = AppSettings(serverUrl, bearerToken)
            return Result.Success(Unit)
        }

        override suspend fun updateLastSyncTime(instant: Instant): Result<Unit> = Result.Success(Unit)

        override suspend fun saveDashboardPreferences(
            showNetWorthSummary: Boolean,
            recentTransactionsCount: Int,
            chartPeriod: DashboardChartPeriod,
            rangeMode: DashboardRangeMode,
            showSpendingInsight: Boolean,
            categoryChartStyle: CategoryChartStyle,
        ): Result<Unit> = Result.Success(Unit)
    }
}
