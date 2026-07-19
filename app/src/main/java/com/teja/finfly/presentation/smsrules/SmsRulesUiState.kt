/* Presentation state for the SMS parsing rules hub. */
package com.teja.finfly.presentation.smsrules

import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.RulesConfig
import com.teja.finfly.domain.model.ParsedTransaction

data class SmsRulesUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val enabled: Boolean = false,
    val bankRules: List<BankRule> = emptyList(),
    val categoryRules: List<CategoryRule> = emptyList(),
    val busy: Boolean = false,
    val importPreview: RulesConfig? = null,
    val feedback: SmsRulesFeedback? = null,
    val scanFromDate: String = "",
    val scanUntilDate: String = "",
    val isScanning: Boolean = false,
    val isPushingPreview: Boolean = false,
    val scanPreview: List<OnDemandSmsPreview> = emptyList(),
    val scanError: OnDemandScanError? = null,
)

data class OnDemandSmsPreview(
    val id: String,
    val transaction: ParsedTransaction,
    val selected: Boolean = true,
    val status: SmsPreviewStatus = SmsPreviewStatus.READY,
    val errorMessage: String? = null,
)

enum class SmsPreviewStatus { READY, DUPLICATE, PUSHED, FAILED }

enum class OnDemandScanError { INVALID_DATE, INVALID_RANGE, READ_FAILED, PUSH_FAILED }

sealed interface SmsRulesFeedback {
    data class Exported(val path: String) : SmsRulesFeedback
    data class Imported(val banks: Int, val categories: Int) : SmsRulesFeedback
    data object ExportFailed : SmsRulesFeedback
    data object ImportFailed : SmsRulesFeedback
    data class ScanComplete(val count: Int) : SmsRulesFeedback
    data class TransactionsPushed(val count: Int) : SmsRulesFeedback
}
