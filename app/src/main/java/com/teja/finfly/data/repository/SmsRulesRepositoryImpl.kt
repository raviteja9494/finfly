/* Data-layer Room implementation for JSON-backed SMS rules. */
package com.teja.finfly.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.teja.finfly.data.local.FinFlyDatabase
import com.teja.finfly.data.local.entity.BankRuleEntity
import com.teja.finfly.data.local.entity.CategoryRuleEntity
import com.teja.finfly.data.sms.DefaultSmsRules
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.RulesConfig
import com.teja.finfly.domain.model.RulesImportMode
import com.teja.finfly.domain.model.RulesImportSummary
import com.teja.finfly.domain.repository.SmsRulesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRulesRepositoryImpl @Inject constructor(
    private val database: FinFlyDatabase,
    private val gson: Gson,
    private val clock: Clock,
) : SmsRulesRepository {
    private val dao get() = database.smsRulesDao()

    override fun observeBankRules(): Flow<Result<List<BankRule>>> = dao.observeBankRules()
        .map { rows -> Result.Success(rows.map { it.toDomain() }) as Result<List<BankRule>> }
        .catch { emit(Result.Error(it.message ?: READ_ERROR, it)) }

    override fun observeCategoryRules(): Flow<Result<List<CategoryRule>>> = dao.observeCategoryRules()
        .map { rows -> Result.Success(rows.map { it.toDomain() }) as Result<List<CategoryRule>> }
        .catch { emit(Result.Error(it.message ?: READ_ERROR, it)) }

    override suspend fun getBankRules(): Result<List<BankRule>> = runCatching {
        dao.getBankRules().map { it.toDomain() }
    }.toResult(READ_ERROR)

    override suspend fun getCategoryRules(): Result<List<CategoryRule>> = runCatching {
        dao.getCategoryRules().map { it.toDomain() }
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

    override suspend fun ensureDefaults(): Result<Unit> = runCatching {
        database.withTransaction {
            if (dao.bankRuleCount() == 0) {
                dao.upsertBankRules(DefaultSmsRules.bankRules(clock.millis()).map { it.toEntity() })
            }
            if (dao.categoryRuleCount() == 0) {
                dao.upsertCategoryRules(DefaultSmsRules.categoryRules().map { it.toEntity() })
            }
        }
    }.toUnitResult(WRITE_ERROR)

    override suspend fun createConfig(exportedAt: Long): Result<RulesConfig> = runCatching {
        RulesConfig(
            exportedAt = exportedAt,
            bankRules = dao.getBankRules().map { it.toDomain() },
            categoryRules = dao.getCategoryRules().map { it.toDomain() },
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
        }
        RulesImportSummary(bankRules.size, categoryRules.size)
    }.toResult(IMPORT_ERROR)

    private fun BankRule.toEntity() = BankRuleEntity(id, name, enabled, gson.toJson(this), updatedAt)
    private fun BankRuleEntity.toDomain(): BankRule = gson.fromJson(configJson, BankRule::class.java)
    private fun CategoryRule.toEntity() = CategoryRuleEntity(id, name, enabled, priority, gson.toJson(this))
    private fun CategoryRuleEntity.toDomain(): CategoryRule = gson.fromJson(configJson, CategoryRule::class.java)

    private fun <T> kotlin.Result<T>.toResult(message: String): Result<T> = fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(it.message ?: message, it) },
    )
    private fun kotlin.Result<Unit>.toUnitResult(message: String): Result<Unit> = toResult(message)

    private companion object {
        const val READ_ERROR = "sms_rules_read_error"
        const val WRITE_ERROR = "sms_rules_write_error"
        const val IMPORT_ERROR = "sms_rules_import_error"
    }
}
