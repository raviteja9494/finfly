/* Domain repository contract for editable SMS rules. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.RulesConfig
import com.teja.finfly.domain.model.RulesImportMode
import com.teja.finfly.domain.model.RulesImportSummary
import kotlinx.coroutines.flow.Flow

/** Persists, observes, seeds, imports, and exports human-readable bank and category rules. */
interface SmsRulesRepository {
    fun observeBankRules(): Flow<Result<List<BankRule>>>
    fun observeCategoryRules(): Flow<Result<List<CategoryRule>>>
    suspend fun getBankRules(): Result<List<BankRule>>
    suspend fun getCategoryRules(): Result<List<CategoryRule>>
    suspend fun saveBankRule(rule: BankRule): Result<Unit>
    suspend fun deleteBankRule(id: String): Result<Unit>
    suspend fun saveCategoryRule(rule: CategoryRule): Result<Unit>
    suspend fun deleteCategoryRule(id: String): Result<Unit>
    suspend fun ensureDefaults(): Result<Unit>
    suspend fun createConfig(exportedAt: Long): Result<RulesConfig>
    suspend fun importConfig(config: RulesConfig, mode: RulesImportMode): Result<RulesImportSummary>
}
