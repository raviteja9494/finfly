/* Domain use case orchestrating current rules and the pure parser. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.BankRule
import com.teja.finflyiii.domain.model.CategoryRule
import com.teja.finflyiii.domain.model.SmsParseResult
import com.teja.finflyiii.domain.model.SmsParserTestReport
import com.teja.finflyiii.domain.repository.SmsRulesRepository
import com.teja.finflyiii.domain.sms.SmsParserFactory
import javax.inject.Inject

/** Loads enabled rules for production parsing and complete, non-destructive configuration testing. */
class SmsParserEngine @Inject constructor(
    private val repository: SmsRulesRepository,
    private val parserFactory: SmsParserFactory,
) {
    suspend fun process(sender: String, message: String, timestamp: Long): SmsParseResult {
        val bankRules = repository.getBankRules().valueOrNull()
            ?: return SmsParseResult.Skipped(REASON_RULES_UNAVAILABLE, message)
        val categoryRules = repository.getCategoryRules().valueOrNull()
            ?: return SmsParseResult.Skipped(REASON_RULES_UNAVAILABLE, message)
        val universalTags = repository.getUniversalTags().valueOrNull()
            ?: return SmsParseResult.Skipped(REASON_RULES_UNAVAILABLE, message)
        return parserFactory.create(
            bankRules.filter(BankRule::enabled),
            categoryRules.filter(CategoryRule::enabled),
            universalTags,
        ).parse(sender, message, timestamp)
    }

    suspend fun testAllRules(sender: String, message: String, timestamp: Long = 0L): SmsParserTestReport {
        val bankRules = repository.getBankRules().valueOrNull()
            ?: return unavailableReport(sender, message)
        val categoryRules = repository.getCategoryRules().valueOrNull()
            ?: return unavailableReport(sender, message)
        val universalTags = repository.getUniversalTags().valueOrNull()
            ?: return unavailableReport(sender, message)
        val enabledBanks = bankRules.filter(BankRule::enabled)
        val enabledCategories = categoryRules.filter(CategoryRule::enabled)
        val trimmedSender = sender.trim()
        if (trimmedSender.isNotEmpty()) {
            val result = parserFactory.create(enabledBanks, enabledCategories, universalTags)
                .parse(trimmedSender, message, timestamp)
            return SmsParserTestReport(
                result = result,
                matches = listOfNotNull((result as? SmsParseResult.Success)?.transaction),
                inferredSender = false,
                checkedBankRules = enabledBanks.size,
                checkedCategoryRules = enabledCategories.size,
                universalTagCount = universalTags.size,
            )
        }

        val attempts = enabledBanks.map { rule ->
            rule to parserFactory.create(listOf(rule), enabledCategories, universalTags)
                .parse(rule.senderIds.firstOrNull().orEmpty(), message, timestamp)
        }
        val matches = attempts.mapNotNull { (_, result) ->
            (result as? SmsParseResult.Success)?.transaction
        }
        val closest = attempts
            .filter { (_, result) -> result is SmsParseResult.Skipped }
            .maxByOrNull { (_, result) -> (result as SmsParseResult.Skipped).reason.progressRank() }
        val result = matches.firstOrNull()?.let { SmsParseResult.Success(it) }
            ?: closest?.second
            ?: SmsParseResult.NoRuleMatched(message, "")
        return SmsParserTestReport(
            result = result,
            matches = matches,
            inferredSender = true,
            checkedBankRules = enabledBanks.size,
            checkedCategoryRules = enabledCategories.size,
            universalTagCount = universalTags.size,
            closestRuleName = closest?.first?.name,
        )
    }

    private fun unavailableReport(sender: String, message: String) = SmsParserTestReport(
        result = SmsParseResult.Skipped(REASON_RULES_UNAVAILABLE, message),
        matches = emptyList(),
        inferredSender = sender.isBlank(),
        checkedBankRules = 0,
        checkedCategoryRules = 0,
        universalTagCount = 0,
    )

    private fun String.progressRank(): Int = when (this) {
        "description_not_found" -> 3
        "amount_not_found" -> 2
        "type_not_found" -> 1
        else -> 0
    }

    private fun <T> Result<T>.valueOrNull(): T? = (this as? Result.Success)?.value

    companion object { const val REASON_RULES_UNAVAILABLE = "rules_unavailable" }
}
