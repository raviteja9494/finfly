/* Data-layer Room implementation for JSON-backed SMS rules. */
package com.teja.finflyiii.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.teja.finflyiii.data.local.FinFlyIIIDatabase
import com.teja.finflyiii.data.local.entity.BankRuleEntity
import com.teja.finflyiii.data.local.entity.CategoryRuleEntity
import com.teja.finflyiii.data.sms.DefaultSmsRules
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.BankRule
import com.teja.finflyiii.domain.model.CategoryRule
import com.teja.finflyiii.domain.model.RulesConfig
import com.teja.finflyiii.domain.model.RulesImportMode
import com.teja.finflyiii.domain.model.RulesImportSummary
import com.teja.finflyiii.domain.repository.SmsRulesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRulesRepositoryImpl @Inject constructor(
    private val database: FinFlyIIIDatabase,
    private val gson: Gson,
    private val clock: Clock,
) : SmsRulesRepository {
    private val dao get() = database.smsRulesDao()

    override fun observeBankRules(): Flow<Result<List<BankRule>>> = dao.observeBankRules()
        .map { rows -> Result.Success(rows.map { it.toDomain() }) as Result<List<BankRule>> }
        .catch { emit(Result.Error(it.message ?: READ_ERROR, it)) }

    override fun observeCategoryRules(): Flow<Result<List<CategoryRule>>> = dao.observeCategoryRules()
        .map { rows -> Result.Success(rows.filterNot { it.id == UNIVERSAL_TAGS_ID }.map { it.toDomain() }) as Result<List<CategoryRule>> }
        .catch { emit(Result.Error(it.message ?: READ_ERROR, it)) }

    override fun observeUniversalTags(): Flow<Result<List<String>>> = dao.observeCategoryRules()
        .map { rows ->
            Result.Success(rows.firstOrNull { it.id == UNIVERSAL_TAGS_ID }?.toDomain()?.fireflyTags.orEmpty()) as Result<List<String>>
        }.catch { emit(Result.Error(it.message ?: READ_ERROR, it)) }

    override suspend fun getBankRules(): Result<List<BankRule>> = runCatching {
        dao.getBankRules().map { it.toDomain() }
    }.toResult(READ_ERROR)

    override suspend fun getCategoryRules(): Result<List<CategoryRule>> = runCatching {
        dao.getCategoryRules().filterNot { it.id == UNIVERSAL_TAGS_ID }.map { it.toDomain() }
    }.toResult(READ_ERROR)

    override suspend fun getUniversalTags(): Result<List<String>> = runCatching {
        dao.getCategoryRules().firstOrNull { it.id == UNIVERSAL_TAGS_ID }?.toDomain()?.fireflyTags.orEmpty()
    }.toResult(READ_ERROR)

    override suspend fun saveBankRule(rule: BankRule): Result<Unit> = runCatching {
        dao.upsertBankRule(rule.toEntity())
    }.toUnitResult(WRITE_ERROR)

    override suspend fun deleteBankRule(id: String): Result<Unit> = runCatching {
        dao.deleteBankRule(id)
    }.toUnitResult(WRITE_ERROR)

    override suspend fun saveCategoryRule(rule: CategoryRule): Result<Unit> = runCatching {
        dao.upsertCategoryRule(rule.toEntity())
    }.toUnitResult(WRITE_ERROR)

    override suspend fun deleteCategoryRule(id: String): Result<Unit> = runCatching {
        dao.deleteCategoryRule(id)
    }.toUnitResult(WRITE_ERROR)

    override suspend fun saveUniversalTags(tags: List<String>): Result<Unit> = runCatching {
        dao.upsertCategoryRule(universalTagsRule(tags).toEntity())
    }.toUnitResult(WRITE_ERROR)

    override suspend fun ensureDefaults(): Result<Unit> = runCatching {
        database.withTransaction {
            if (dao.bankRuleCount() == 0) {
                dao.upsertBankRules(DefaultSmsRules.bankRules(clock.millis()).map { it.toEntity() })
            }
            if (dao.categoryRuleCount() == 0) {
                dao.upsertCategoryRules(DefaultSmsRules.categoryRules().map { it.toEntity() })
            }
            val storedCategories = dao.getCategoryRules()
            val universal = storedCategories.firstOrNull { it.id == UNIVERSAL_TAGS_ID }
                ?.toDomain()?.fireflyTags.orEmpty()
            val legacyGlobal = storedCategories.filterNot { it.id == UNIVERSAL_TAGS_ID }
                .map { it.toDomain() }
                .filter(CategoryRule::applyTagsToAll)
                .flatMap { it.fireflyTags.orEmpty() }
            val migrated = (universal + legacyGlobal).distinctBy(String::lowercase)
            if (migrated.isNotEmpty()) dao.upsertCategoryRule(universalTagsRule(migrated).toEntity())
        }
    }.toUnitResult(WRITE_ERROR)

    override suspend fun createConfig(exportedAt: Long): Result<RulesConfig> = runCatching {
        RulesConfig(
            exportedAt = exportedAt,
            bankRules = dao.getBankRules().map { it.toDomain() },
            categoryRules = dao.getCategoryRules().filterNot { it.id == UNIVERSAL_TAGS_ID }.map { it.toDomain() },
            universalTags = dao.getCategoryRules().firstOrNull { it.id == UNIVERSAL_TAGS_ID }
                ?.toDomain()?.fireflyTags.orEmpty(),
        )
    }.toResult(READ_ERROR)

    override suspend fun importConfig(
        config: RulesConfig,
        mode: RulesImportMode,
    ): Result<RulesImportSummary> = runCatching {
        require(config.version == RulesConfig.CURRENT_VERSION)
        var bankRules = config.bankRules
        var categoryRules = config.categoryRules
        database.withTransaction {
            if (mode == RulesImportMode.REPLACE) {
                dao.clearBankRules()
                dao.clearCategoryRules()
            } else {
                val bankNames = dao.getBankRules().map { it.name.lowercase() }.toSet()
                val categoryNames = dao.getCategoryRules().map { it.name.lowercase() }.toSet()
                bankRules = bankRules.filterNot { it.name.lowercase() in bankNames }
                categoryRules = categoryRules.filterNot { it.name.lowercase() in categoryNames }
            }
            dao.upsertBankRules(bankRules.map { it.toEntity() })
            dao.upsertCategoryRules(categoryRules.map { it.toEntity() })
            val importedUniversalTags = if (mode == RulesImportMode.MERGE) {
                (dao.getCategoryRules().firstOrNull { it.id == UNIVERSAL_TAGS_ID }
                    ?.toDomain()?.fireflyTags.orEmpty() + config.universalTags)
                    .distinctBy(String::lowercase)
            } else config.universalTags
            if (importedUniversalTags.isNotEmpty()) dao.upsertCategoryRule(universalTagsRule(importedUniversalTags).toEntity())
        }
        RulesImportSummary(bankRules.size, categoryRules.size)
    }.toResult(IMPORT_ERROR)

    private fun BankRule.toEntity() = BankRuleEntity(id, name, enabled, gson.toJson(this), updatedAt)
    private fun BankRuleEntity.toDomain(): BankRule = gson.fromJson(configJson, BankRule::class.java).let { rule ->
        rule.copy(fireflyTags = rule.fireflyTags.orEmpty())
    }
    private fun CategoryRule.toEntity() = CategoryRuleEntity(id, name, enabled, priority, gson.toJson(this))
    private fun CategoryRuleEntity.toDomain(): CategoryRule =
        gson.fromJson(configJson, CategoryRule::class.java).let { rule ->
            rule.copy(fireflyTags = rule.fireflyTags.orEmpty())
        }

    private fun universalTagsRule(tags: List<String>) = CategoryRule(
        id = UNIVERSAL_TAGS_ID,
        name = UNIVERSAL_TAGS_NAME,
        keywords = emptyList(),
        fireflyCategory = "",
        priority = Int.MAX_VALUE,
        enabled = true,
        fireflyTags = tags.map(String::trim).filter(String::isNotBlank).distinctBy(String::lowercase),
    )

    private fun <T> kotlin.Result<T>.toResult(message: String): Result<T> = fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(it.message ?: message, it) },
    )
    private fun kotlin.Result<Unit>.toUnitResult(message: String): Result<Unit> = toResult(message)

    private companion object {
        const val READ_ERROR = "sms_rules_read_error"
        const val WRITE_ERROR = "sms_rules_write_error"
        const val IMPORT_ERROR = "sms_rules_import_error"
        const val UNIVERSAL_TAGS_ID = "__finfly_universal_tags__"
        const val UNIVERSAL_TAGS_NAME = "Universal tags"
    }
}
