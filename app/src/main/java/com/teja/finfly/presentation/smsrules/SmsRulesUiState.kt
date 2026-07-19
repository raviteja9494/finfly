/* Presentation state for the SMS parsing rules hub. */
package com.teja.finfly.presentation.smsrules

import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.RulesConfig

data class SmsRulesUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val enabled: Boolean = false,
    val bankRules: List<BankRule> = emptyList(),
    val categoryRules: List<CategoryRule> = emptyList(),
    val busy: Boolean = false,
    val importPreview: RulesConfig? = null,
    val feedback: SmsRulesFeedback? = null,
)

sealed interface SmsRulesFeedback {
    data class Exported(val path: String) : SmsRulesFeedback
    data class Imported(val banks: Int, val categories: Int) : SmsRulesFeedback
    data object ExportFailed : SmsRulesFeedback
    data object ImportFailed : SmsRulesFeedback
}
