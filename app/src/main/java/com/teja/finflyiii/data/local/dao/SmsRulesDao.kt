/* Data-layer Room access for editable SMS rules. */
package com.teja.finflyiii.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teja.finflyiii.data.local.entity.BankRuleEntity
import com.teja.finflyiii.data.local.entity.CategoryRuleEntity
import com.teja.finflyiii.data.local.entity.TagRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsRulesDao {
    @Query("SELECT * FROM bank_rules ORDER BY name COLLATE NOCASE")
    fun observeBankRules(): Flow<List<BankRuleEntity>>

    @Query("SELECT * FROM category_rules ORDER BY priority, name COLLATE NOCASE")
    fun observeCategoryRules(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM tag_rules ORDER BY name COLLATE NOCASE")
    fun observeTagRules(): Flow<List<TagRuleEntity>>

    @Query("SELECT * FROM bank_rules ORDER BY name COLLATE NOCASE")
    suspend fun getBankRules(): List<BankRuleEntity>

    @Query("SELECT * FROM category_rules ORDER BY priority, name COLLATE NOCASE")
    suspend fun getCategoryRules(): List<CategoryRuleEntity>

    @Query("SELECT * FROM tag_rules ORDER BY name COLLATE NOCASE")
    suspend fun getTagRules(): List<TagRuleEntity>

    @Query("SELECT COUNT(*) FROM bank_rules")
    suspend fun bankRuleCount(): Int

    @Query("SELECT COUNT(*) FROM category_rules")
    suspend fun categoryRuleCount(): Int

    @Query("SELECT COUNT(*) FROM tag_rules")
    suspend fun tagRuleCount(): Int

    @Upsert
    suspend fun upsertBankRule(rule: BankRuleEntity)

    @Upsert
    suspend fun upsertBankRules(rules: List<BankRuleEntity>)

    @Upsert
    suspend fun upsertCategoryRule(rule: CategoryRuleEntity)

    @Upsert
    suspend fun upsertCategoryRules(rules: List<CategoryRuleEntity>)

    @Upsert
    suspend fun upsertTagRule(rule: TagRuleEntity)

    @Upsert
    suspend fun upsertTagRules(rules: List<TagRuleEntity>)

    @Query("DELETE FROM bank_rules WHERE id = :id")
    suspend fun deleteBankRule(id: String)

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteCategoryRule(id: String)

    @Query("DELETE FROM tag_rules WHERE id = :id")
    suspend fun deleteTagRule(id: String)

    @Query("DELETE FROM bank_rules")
    suspend fun clearBankRules()

    @Query("DELETE FROM category_rules")
    suspend fun clearCategoryRules()

    @Query("DELETE FROM tag_rules")
    suspend fun clearTagRules()
}
