/* Domain use case orchestrating current rules and the pure parser. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.SmsParseResult
import com.teja.finfly.domain.repository.SmsRulesRepository
import com.teja.finfly.domain.sms.SmsParserFactory
import javax.inject.Inject

/** Loads enabled rules for each message and exposes deterministic unsaved-rule testing. */
class SmsParserEngine @Inject constructor(
    private val repository: SmsRulesRepository,
    private val parserFactory: SmsParserFactory,
) {
    @Volatile private var latestCategories: List<CategoryRule> = emptyList()

    suspend fun process(sender: String, message: String, timestamp: Long): SmsParseResult {
        val bankRules = repository.getBankRules().valueOrNull()
            ?: return SmsParseResult.Skipped(REASON_RULES_UNAVAILABLE, message)
        val categoryRules = repository.getCategoryRules().valueOrNull()
            ?: return SmsParseResult.Skipped(REASON_RULES_UNAVAILABLE, message)
        val universalTags = repository.getUniversalTags().valueOrNull()
            ?: return SmsParseResult.Skipped(REASON_RULES_UNAVAILABLE, message)
        latestCategories = categoryRules.filter(CategoryRule::enabled)
        return parserFactory.create(
            bankRules.filter(BankRule::enabled),
            latestCategories,
            universalTags,
        ).parse(sender, message, timestamp)
    }

    fun testRule(rule: BankRule, sampleSms: String): SmsParseResult =
        parserFactory.create(listOf(rule.copy(enabled = true)), latestCategories)
            .parse(rule.senderIds.firstOrNull().orEmpty(), sampleSms, 0L)

    fun testRule(
        rule: BankRule,
        sampleSms: String,
        categoryRules: List<CategoryRule>,
    ): SmsParseResult = parserFactory.create(listOf(rule.copy(enabled = true)), categoryRules)
        .parse(rule.senderIds.firstOrNull().orEmpty(), sampleSms, 0L)

    private fun <T> Result<T>.valueOrNull(): T? = (this as? Result.Success)?.value

    companion object { const val REASON_RULES_UNAVAILABLE = "rules_unavailable" }
}
