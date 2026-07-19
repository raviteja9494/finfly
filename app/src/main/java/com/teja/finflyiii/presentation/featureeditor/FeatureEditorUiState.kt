/* Presentation-layer form state for creating secondary Firefly resources. */
package com.teja.finflyiii.presentation.featureeditor

import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.FireflyFeature
import com.teja.finflyiii.domain.model.FireflyRuleClause
import com.teja.finflyiii.domain.model.FireflyRuleGroup

data class FeatureEditorUiState(
    val feature: FireflyFeature,
    val itemId: String? = null,
    val isLoading: Boolean = false,
    val name: String = "",
    val notes: String = "",
    val minimumAmount: String = "",
    val maximumAmount: String = "",
    val currencyCode: String = "",
    val startDate: String = "",
    val targetDate: String = "",
    val accountId: String = "",
    val repeatFrequency: String = "monthly",
    val accounts: List<Account> = emptyList(),
    val ruleGroups: List<FireflyRuleGroup> = emptyList(),
    val ruleGroupId: String = "",
    val active: Boolean = true,
    val strict: Boolean = true,
    val stopProcessing: Boolean = false,
    val ruleTriggers: List<FireflyRuleClause> = listOf(FireflyRuleClause(type = "description_contains")),
    val ruleActions: List<FireflyRuleClause> = listOf(FireflyRuleClause(type = "set_category")),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: FeatureEditorError? = null,
    val errorDetails: String? = null,
)

enum class FeatureEditorError {
    NAME_REQUIRED,
    INVALID_AMOUNT,
    MAXIMUM_BELOW_MINIMUM,
    CURRENCY_REQUIRED,
    INVALID_CURRENCY,
    INVALID_DATE,
    TARGET_DATE_BEFORE_START,
    ACCOUNT_REQUIRED,
    SAVE_FAILED,
    LOAD_FAILED,
    RULE_GROUP_REQUIRED,
    RULE_CLAUSE_REQUIRED,
}
