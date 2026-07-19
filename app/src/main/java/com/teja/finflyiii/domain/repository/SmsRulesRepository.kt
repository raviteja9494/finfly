/* Domain repository contract for editable SMS rules. */
package com.teja.finflyiii.domain.repository

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.BankRule
import com.teja.finflyiii.domain.model.CategoryRule
import com.teja.finflyiii.domain.model.RulesConfig
import com.teja.finflyiii.domain.model.RulesImportMode
import com.teja.finflyiii.domain.model.RulesImportSummary
import kotlinx.coroutines.flow.Flow

/** Persists, observes, seeds, imports, and exports human-readable bank and category rules. */
interface SmsRulesRepository {
    fun observeBankRules(): Flow<Result<List<BankRule>>>
    fun observeCategoryRules(): Flow<Result<List<CategoryRule>>>
    fun observeUniversalTags(): Flow<Result<List<String>>>
    suspend fun getBankRules(): Result<List<BankRule>>
    suspend fun getCategoryRules(): Result<List<CategoryRule>>
    suspend fun getUniversalTags(): Result<List<String>>
    suspend fun saveBankRule(rule: BankRule): Result<Unit>
    suspend fun deleteBankRule(id: String): Result<Unit>
    suspend fun saveCategoryRule(rule: CategoryRule): Result<Unit>
    suspend fun deleteCategoryRule(id: String): Result<Unit>
    suspend fun saveUniversalTags(tags: List<String>): Result<Unit>
    suspend fun ensureDefaults(): Result<Unit>
    suspend fun createConfig(exportedAt: Long): Result<RulesConfig>
    suspend fun importConfig(config: RulesConfig, mode: RulesImportMode): Result<RulesImportSummary>
}
