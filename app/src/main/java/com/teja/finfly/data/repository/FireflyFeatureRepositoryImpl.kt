/* Data-layer remote implementation for secondary Firefly feature lists. */
package com.teja.finfly.data.repository

import com.teja.finfly.data.network.FireflyApiService
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.FireflyFeature
import com.teja.finfly.domain.model.FireflyFeatureItem
import com.teja.finfly.domain.model.FireflyFeatureDraft
import com.teja.finfly.domain.repository.FireflyFeatureRepository
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.data.network.dto.StoreBillRequest
import com.teja.finfly.data.network.dto.StoreBudgetRequest
import com.teja.finfly.data.network.dto.StoreCategoryRequest
import com.teja.finfly.data.network.dto.StorePiggyBankAccountRequest
import com.teja.finfly.data.network.dto.StorePiggyBankRequest
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

    override suspend fun create(draft: FireflyFeatureDraft): Result<FireflyFeatureItem> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            when (draft) {
                is FireflyFeatureDraft.Budget -> {
                    val amount = draft.monthlyAmount
                    val resource = api.createBudget(
                        StoreBudgetRequest(
                            name = draft.name.trim(),
                            notes = draft.notes.trim().takeIf(String::isNotBlank),
                            autoBudgetType = amount?.let { "reset" },
                            autoBudgetCurrencyCode = amount?.let {
                                draft.currencyCode.trim().uppercase().takeIf(String::isNotBlank)
                            },
                            autoBudgetAmount = amount?.toPlainString(),
                            autoBudgetPeriod = amount?.let { "monthly" },
                        )
                    ).data
                    FireflyFeatureItem(
                        resource.id,
                        resource.attributes.name,
                        listOfNotNull(resource.attributes.autoBudgetAmount, resource.attributes.currencyCode),
                    )
                }
                is FireflyFeatureDraft.Category -> {
                    val resource = api.createCategory(
                        StoreCategoryRequest(
                            name = draft.name.trim(),
                            notes = draft.notes.trim().takeIf(String::isNotBlank),
                        )
                    ).data
                    FireflyFeatureItem(resource.id, resource.attributes.name)
                }
                is FireflyFeatureDraft.Bill -> {
                    val resource = api.createBill(
                        StoreBillRequest(
                            name = draft.name.trim(),
                            amountMin = draft.minimumAmount.toPlainString(),
                            amountMax = draft.maximumAmount.toPlainString(),
                            date = draft.firstDueDate.atStartOfDay(ZoneId.systemDefault())
                                .toOffsetDateTime().toString(),
                            repeatFrequency = draft.repeatFrequency,
                            currencyCode = draft.currencyCode.trim().uppercase(),
                            notes = draft.notes.trim().takeIf(String::isNotBlank),
                        )
                    ).data
                    FireflyFeatureItem(
                        resource.id,
                        resource.attributes.name,
                        listOfNotNull(
                            resource.attributes.amountMin,
                            resource.attributes.amountMax,
                            resource.attributes.currencyCode,
                            resource.attributes.nextExpectedMatch,
                        ),
                    )
                }
                is FireflyFeatureDraft.PiggyBank -> {
                    val resource = api.createPiggyBank(
                        StorePiggyBankRequest(
                            name = draft.name.trim(),
                            accounts = listOf(
                                StorePiggyBankAccountRequest(
                                    id = draft.accountId,
                                    currentAmount = draft.currentAmount?.toPlainString(),
                                )
                            ),
                            targetAmount = draft.targetAmount.toPlainString(),
                            currentAmount = draft.currentAmount?.toPlainString(),
                            startDate = draft.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            targetDate = draft.targetDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            notes = draft.notes.trim().takeIf(String::isNotBlank),
                        )
                    ).data
                    FireflyFeatureItem(
                        resource.id,
                        resource.attributes.name,
                        listOfNotNull(
                            resource.attributes.currentAmount,
                            resource.attributes.targetAmount,
                            resource.attributes.currencyCode,
                        ),
                        resource.attributes.percentage,
                    )
                }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: SAVE_ERROR, it) },
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
        const val SAVE_ERROR = "feature_save_error"
    }
}
