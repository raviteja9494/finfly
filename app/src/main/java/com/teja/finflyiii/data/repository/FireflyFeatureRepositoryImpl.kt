/* Data-layer remote implementation for secondary Firefly feature lists. */
package com.teja.finflyiii.data.repository

import com.teja.finflyiii.data.network.FireflyApiService
import com.teja.finflyiii.data.network.fireflyMessage
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.FireflyFeature
import com.teja.finflyiii.domain.model.FireflyFeatureItem
import com.teja.finflyiii.domain.model.FireflyFeatureDraft
import com.teja.finflyiii.domain.repository.FireflyFeatureRepository
import com.teja.finflyiii.domain.repository.SettingsRepository
import com.teja.finflyiii.data.network.dto.StoreBillRequest
import com.teja.finflyiii.data.network.dto.StoreBudgetRequest
import com.teja.finflyiii.data.network.dto.StoreCategoryRequest
import com.teja.finflyiii.data.network.dto.StorePiggyBankAccountRequest
import com.teja.finflyiii.data.network.dto.StorePiggyBankRequest
import com.teja.finflyiii.data.network.dto.StoreTagRequest
import com.teja.finflyiii.data.network.dto.UpdateCategoryRequest
import com.teja.finflyiii.data.network.dto.UpdateTagRequest
import com.teja.finflyiii.data.network.dto.UpdatePiggyBankRequest
import com.teja.finflyiii.data.network.dto.UpdatePiggyBankAccountRequest
import com.teja.finflyiii.data.network.dto.StoreRuleRequest
import com.teja.finflyiii.data.network.dto.RuleClauseDto
import com.teja.finflyiii.domain.model.FireflyRuleClause
import com.teja.finflyiii.domain.model.FireflyRuleGroup
import com.teja.finflyiii.data.local.FinFlyIIIDatabase
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
    private val database: FinFlyIIIDatabase,
) : FireflyFeatureRepository {
    override suspend fun load(feature: FireflyFeature): Result<List<FireflyFeatureItem>> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            when (feature) {
                FireflyFeature.BUDGETS -> loadBudgets()
                FireflyFeature.CATEGORIES -> api.getCategories(limit = PAGE_SIZE).data.map {
                    FireflyFeatureItem(it.id, it.attributes.name)
                }
                FireflyFeature.TAGS -> api.getTags(limit = PAGE_SIZE).data.map {
                    FireflyFeatureItem(it.id, it.attributes.tag)
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
                FireflyFeature.RULES -> api.getRules(limit = PAGE_SIZE).data.map {
                    FireflyFeatureItem(
                        it.id,
                        it.attributes.title,
                        listOfNotNull(it.attributes.ruleGroupTitle, it.attributes.description),
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
                is FireflyFeatureDraft.Tag -> {
                    val resource = api.createTag(
                        StoreTagRequest(
                            tag = draft.name.trim(),
                            description = draft.notes.trim().takeIf(String::isNotBlank),
                        )
                    ).data
                    FireflyFeatureItem(resource.id, resource.attributes.tag)
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
                is FireflyFeatureDraft.Rule -> api.createRule(draft.toRequest()).data.toItem()
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.fireflyMessage(SAVE_ERROR), it) },
        )
    }

    override suspend fun loadDraft(feature: FireflyFeature, id: String): Result<FireflyFeatureDraft> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            when (feature) {
                FireflyFeature.BUDGETS -> api.getBudget(id).data.attributes.run {
                    FireflyFeatureDraft.Budget(
                        name,
                        notes.orEmpty(),
                        autoBudgetAmount?.toBigDecimalOrNull(),
                        autoBudgetCurrencyCode ?: currencyCode.orEmpty(),
                    )
                }
                FireflyFeature.CATEGORIES -> api.getCategory(id).data.attributes.run {
                    FireflyFeatureDraft.Category(name, notes.orEmpty())
                }
                FireflyFeature.TAGS -> api.getTag(id).data.attributes.run {
                    FireflyFeatureDraft.Tag(tag, description.orEmpty())
                }
                FireflyFeature.BILLS -> api.getBill(id).data.attributes.run {
                    FireflyFeatureDraft.Bill(
                        name,
                        notes.orEmpty(),
                        amountMin?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                        amountMax?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                        currencyCode.orEmpty(),
                        date.toLocalDateOrToday(),
                        repeatFrequency ?: "monthly",
                    )
                }
                FireflyFeature.PIGGY_BANKS -> api.getPiggyBank(id).data.attributes.run {
                    FireflyFeatureDraft.PiggyBank(
                        name,
                        notes.orEmpty(),
                        accounts.firstOrNull()?.accountId.orEmpty(),
                        targetAmount?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                        currentAmount?.toBigDecimalOrNull(),
                        startDate.toLocalDateOrToday(),
                        targetDate.toLocalDateOrNull(),
                    )
                }
                FireflyFeature.RULES -> api.getRule(id).data.attributes.run {
                    FireflyFeatureDraft.Rule(
                        name = title,
                        notes = description.orEmpty(),
                        groupId = ruleGroupId,
                        active = active,
                        strict = strict,
                        stopProcessing = stopProcessing,
                        triggers = triggers.map { it.toDomain() },
                        actions = actions.map { it.toDomain() },
                    )
                }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: LOAD_ERROR, it) },
        )
    }

    override suspend fun update(id: String, draft: FireflyFeatureDraft): Result<FireflyFeatureItem> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            when (draft) {
                is FireflyFeatureDraft.Budget -> api.updateBudget(
                    id,
                    StoreBudgetRequest(
                        name = draft.name.trim(),
                        notes = draft.notes.trim().takeIf(String::isNotBlank),
                        autoBudgetType = draft.monthlyAmount?.let { "reset" },
                        autoBudgetCurrencyCode = draft.currencyCode.trim().uppercase().takeIf(String::isNotBlank),
                        autoBudgetAmount = draft.monthlyAmount?.toPlainString(),
                        autoBudgetPeriod = draft.monthlyAmount?.let { "monthly" },
                    ),
                ).data.run { FireflyFeatureItem(id, attributes.name) }
                is FireflyFeatureDraft.Category -> api.updateCategory(
                    id, UpdateCategoryRequest(draft.name.trim(), draft.notes.trim().takeIf(String::isNotBlank)),
                ).data.run { FireflyFeatureItem(id, attributes.name) }
                is FireflyFeatureDraft.Tag -> api.updateTag(
                    id, UpdateTagRequest(draft.name.trim(), draft.notes.trim().takeIf(String::isNotBlank)),
                ).data.run { FireflyFeatureItem(id, attributes.tag) }
                is FireflyFeatureDraft.Bill -> api.updateBill(id, draft.toBillRequest()).data.run {
                    FireflyFeatureItem(id, attributes.name)
                }
                is FireflyFeatureDraft.PiggyBank -> api.updatePiggyBank(id, draft.toPiggyBankUpdateRequest()).data.run {
                    FireflyFeatureItem(id, attributes.name, progressPercent = attributes.percentage)
                }
                is FireflyFeatureDraft.Rule -> api.updateRule(id, draft.toRequest()).data.toItem()
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.fireflyMessage(SAVE_ERROR), it) },
        )
    }

    override suspend fun loadRuleGroups(): Result<List<FireflyRuleGroup>> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            api.getRuleGroups(limit = PAGE_SIZE).data.filter { it.attributes.active }
                .map { FireflyRuleGroup(it.id, it.attributes.title) }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: LOAD_ERROR, it) },
        )
    }

    override suspend fun delete(feature: FireflyFeature, id: String): Result<Unit> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            val response = when (feature) {
                FireflyFeature.BUDGETS -> api.deleteBudget(id)
                FireflyFeature.CATEGORIES -> api.deleteCategory(id)
                FireflyFeature.TAGS -> api.deleteTag(id)
                FireflyFeature.BILLS -> api.deleteBill(id)
                FireflyFeature.PIGGY_BANKS -> api.deletePiggyBank(id)
                FireflyFeature.RULES -> api.deleteRule(id)
            }
            check(response.isSuccessful)
            when (feature) {
                FireflyFeature.CATEGORIES -> database.categoryDao().delete(id)
                FireflyFeature.TAGS -> database.tagDao().delete(id)
                else -> Unit
            }
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: DELETE_ERROR, it) }
    }

    private suspend fun loadBudgets(): List<FireflyFeatureItem> {
        val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
        val response = api.getBudgets(
            limit = PAGE_SIZE,
            start = today.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
            end = today.withDayOfMonth(today.lengthOfMonth()).format(DateTimeFormatter.ISO_LOCAL_DATE),
        )
        val limits = api.getBudgetLimits(
            start = today.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
            end = today.withDayOfMonth(today.lengthOfMonth()).format(DateTimeFormatter.ISO_LOCAL_DATE),
        ).data.groupBy { it.attributes.budgetId }
        return response.data.map {
            val budgetLimits = limits[it.id].orEmpty()
            val currency = budgetLimits.firstNotNullOfOrNull { limit -> limit.attributes.currencyCode }
                ?: it.attributes.currencyCode.orEmpty()
            val set = budgetLimits.sumOf { limit -> limit.attributes.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
                .takeIf { value -> value.signum() != 0 }
                ?: it.attributes.autoBudgetAmount?.toBigDecimalOrNull()
            val spent = budgetLimits.flatMap { limit -> limit.attributes.spent }.sumOf { row ->
                row.sum.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
            }.takeIf { value -> value.signum() != 0 }
                ?: it.attributes.spent.sumOf { row -> row.sum.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
            FireflyFeatureItem(
                id = it.id,
                title = it.attributes.name,
                setAmount = set,
                spentAmount = spent,
                currencyCode = currency,
                progressPercent = if (set != null && set.signum() > 0) {
                    spent.multiply(java.math.BigDecimal(100)).divide(set, 0, java.math.RoundingMode.HALF_UP).toInt()
                } else null,
            )
        }
    }

    private fun FireflyFeatureDraft.Bill.toBillRequest() = StoreBillRequest(
        name = name.trim(),
        amountMin = minimumAmount.toPlainString(),
        amountMax = maximumAmount.toPlainString(),
        date = firstDueDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime().toString(),
        repeatFrequency = repeatFrequency,
        currencyCode = currencyCode.trim().uppercase(),
        notes = notes.trim().takeIf(String::isNotBlank),
    )

    private fun FireflyFeatureDraft.PiggyBank.toPiggyBankRequest() = StorePiggyBankRequest(
        name = name.trim(),
        accounts = listOf(StorePiggyBankAccountRequest(accountId, currentAmount?.toPlainString())),
        targetAmount = targetAmount.toPlainString(),
        currentAmount = currentAmount?.toPlainString(),
        startDate = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
        targetDate = targetDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
        notes = notes.trim().takeIf(String::isNotBlank),
    )

    private fun FireflyFeatureDraft.PiggyBank.toPiggyBankUpdateRequest() = UpdatePiggyBankRequest(
        name = name.trim(),
        accounts = listOf(UpdatePiggyBankAccountRequest(accountId, currentAmount?.toPlainString())),
        targetAmount = targetAmount.toPlainString(),
        startDate = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
        targetDate = targetDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
        notes = notes.trim().takeIf(String::isNotBlank),
    )

    private fun FireflyFeatureDraft.Rule.toRequest() = StoreRuleRequest(
        title = name.trim(),
        description = notes.trim().takeIf(String::isNotBlank),
        ruleGroupId = groupId,
        active = active,
        strict = strict,
        stopProcessing = stopProcessing,
        triggers = triggers.map { it.toDto() },
        actions = actions.map { it.toDto() },
    )

    private fun FireflyRuleClause.toDto() = RuleClauseDto(id, type, value, active, prohibited, stopProcessing)
    private fun RuleClauseDto.toDomain() = FireflyRuleClause(id, type, value.orEmpty(), active, prohibited, stopProcessing)
    private fun com.teja.finflyiii.data.network.dto.RuleResource.toItem() = FireflyFeatureItem(
        id, attributes.title, listOfNotNull(attributes.ruleGroupTitle, attributes.description),
    )

    private fun String?.toLocalDateOrNull(): java.time.LocalDate? = this?.let { value ->
        runCatching { java.time.OffsetDateTime.parse(value).toLocalDate() }
            .recoverCatching { java.time.LocalDate.parse(value.take(10)) }.getOrNull()
    }
    private fun String?.toLocalDateOrToday(): java.time.LocalDate = toLocalDateOrNull()
        ?: clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()

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
        const val DELETE_ERROR = "feature_delete_error"
    }
}
