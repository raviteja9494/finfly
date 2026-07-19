/* Pure domain models for configurable SMS parsing rules and transfer files. */
package com.teja.finfly.domain.model

data class BankRule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val senderIds: List<String>,
    val accountName: String,
    val fireflyAccountId: String,
    val debitKeywords: List<String>,
    val creditKeywords: List<String>,
    val amountPatterns: List<String>,
    val descriptionPatterns: List<String>,
    val referencePatterns: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CategoryRule(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val fireflyCategory: String,
    val priority: Int,
    val enabled: Boolean,
)

sealed interface SmsParseResult {
    data class Success(val transaction: ParsedTransaction) : SmsParseResult
    data class Skipped(val reason: String, val sms: String) : SmsParseResult
    data class NoRuleMatched(val sms: String, val sender: String) : SmsParseResult
}

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val reference: String,
    val accountName: String,
    val fireflyAccountId: String,
    val category: String,
    val rawSms: String,
    val sender: String,
    val timestamp: Long,
    val matchedRule: String,
)

data class RulesConfig(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val bankRules: List<BankRule>,
    val categoryRules: List<CategoryRule>,
) {
    companion object { const val CURRENT_VERSION = 1 }
}

enum class RulesImportMode { MERGE, REPLACE }

data class RulesImportSummary(
    val bankRulesImported: Int,
    val categoryRulesImported: Int,
)
