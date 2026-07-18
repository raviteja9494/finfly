/* Data-layer remote implementation for secondary Firefly feature lists. */
package com.teja.finfly.data.repository

import com.teja.finfly.data.network.FireflyApiService
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.FireflyFeature
import com.teja.finfly.domain.model.FireflyFeatureItem
import com.teja.finfly.domain.repository.FireflyFeatureRepository
import com.teja.finfly.domain.repository.SettingsRepository
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Maps official Firefly list envelopes into compact drawer-screen summaries. */
@Singleton
class FireflyFeatureRepositoryImpl @Inject constructor(
    private val api: FireflyApiService,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : FireflyFeatureRepository {
    override suspend fun load(feature: FireflyFeature): Result<List<FireflyFeatureItem>> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            when (feature) {
                FireflyFeature.BUDGETS -> loadBudgets()
                FireflyFeature.CATEGORIES -> api.getCategories(limit = PAGE_SIZE).data.map {
                    FireflyFeatureItem(it.id, it.attributes.name)
                }
                FireflyFeature.BILLS -> loadBills()
                FireflyFeature.PIGGY_BANKS -> api.getPiggyBanks(limit = PAGE_SIZE).data.map {
                    val attributes = it.attributes
                    FireflyFeatureItem(
                        id = it.id,
                        title = attributes.name,
                        details = listOfNotNull(
                            attributes.currentAmount,
                            attributes.targetAmount,
                            attributes.currencyCode,
                        ),
                        progressPercent = attributes.percentage,
                    )
                }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: LOAD_ERROR, it) },
        )
    }

    private suspend fun loadBudgets(): List<FireflyFeatureItem> {
        val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
        val response = api.getBudgets(
            limit = PAGE_SIZE,
            start = today.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
            end = today.withDayOfMonth(today.lengthOfMonth()).format(DateTimeFormatter.ISO_LOCAL_DATE),
        )
        return response.data.map {
            FireflyFeatureItem(
                id = it.id,
                title = it.attributes.name,
                details = listOfNotNull(it.attributes.autoBudgetAmount, it.attributes.currencyCode),
            )
        }
    }

    private suspend fun loadBills(): List<FireflyFeatureItem> = api.getBills(limit = PAGE_SIZE).data.map {
        FireflyFeatureItem(
            id = it.id,
            title = it.attributes.name,
            details = listOfNotNull(
                it.attributes.amountMin,
                it.attributes.amountMax,
                it.attributes.currencyCode,
                it.attributes.nextExpectedMatch,
            ),
        )
    }

    private fun isConfigured(): Boolean = settingsRepository.settings.value.run {
        serverUrl.isNotBlank() && bearerToken.isNotBlank()
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val NOT_CONFIGURED = "not_configured"
        const val LOAD_ERROR = "feature_load_error"
    }
}
