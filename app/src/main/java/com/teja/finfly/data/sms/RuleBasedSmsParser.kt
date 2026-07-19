/* Data-layer implementation of configurable, friendly-pattern SMS parsing. */
package com.teja.finfly.data.sms

import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.ParsedTransaction
import com.teja.finfly.domain.model.SmsParseResult
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.domain.sms.SmsParser
import com.teja.finfly.domain.sms.SmsParserFactory
import javax.inject.Inject
import javax.inject.Singleton

class RuleBasedSmsParser(
    private val bankRules: List<BankRule>,
    private val categoryRules: List<CategoryRule>,
) : SmsParser {
    override fun canParse(sender: String, message: String): Boolean = matchingRules(sender).isNotEmpty()

    override fun parse(sender: String, message: String, timestamp: Long): SmsParseResult {
        val senderMatches = matchingRules(sender)
        if (senderMatches.isEmpty()) return SmsParseResult.NoRuleMatched(message, sender)
        val selected = senderMatches.firstOrNull { it.detectType(message) != null }
            ?: return SmsParseResult.Skipped(REASON_TYPE_NOT_FOUND, message)
        val type = selected.detectType(message)
            ?: return SmsParseResult.Skipped(REASON_TYPE_NOT_FOUND, message)
        val amount = extract(message, selected.amountPatterns, AMOUNT_TOKEN, AMOUNT_CAPTURE)
            ?.replace(",", "")?.toDoubleOrNull()
            ?: return SmsParseResult.Skipped(REASON_AMOUNT_NOT_FOUND, message)
        val description = extract(
            message,
            selected.descriptionPatterns,
            DESCRIPTION_TOKEN,
            DESCRIPTION_CAPTURE,
        )?.trim(' ', '.', ',', '-', ':')
            ?.takeIf(String::isNotBlank)
            ?: return SmsParseResult.Skipped(REASON_DESCRIPTION_NOT_FOUND, message)
        val reference = extract(message, selected.referencePatterns, REFERENCE_TOKEN, REFERENCE_CAPTURE)
            ?.trim().orEmpty()
        val enabledCategoryRules = categoryRules.asSequence()
            .filter(CategoryRule::enabled)
            .sortedBy(CategoryRule::priority)
            .toList()
        val category = enabledCategoryRules.asSequence()
            .firstOrNull { rule ->
                rule.fireflyCategory.isNotBlank() && rule.matches(description)
            }?.fireflyCategory.orEmpty()
        val tags = enabledCategoryRules.asSequence()
            .filter { it.applyTagsToAll || it.matches(description) }
            .flatMap { it.fireflyTags.asSequence() }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
            .toList()
        return SmsParseResult.Success(
            ParsedTransaction(
                amount = amount,
                type = type,
                description = description,
                reference = reference,
                accountName = selected.accountName,
                fireflyAccountId = selected.fireflyAccountId,
                category = category,
                tags = tags,
                rawSms = message,
                sender = sender,
                timestamp = timestamp,
                matchedRule = selected.name,
            )
        )
    }

    private fun matchingRules(sender: String): List<BankRule> {
        val enabled = bankRules.filter(BankRule::enabled)
        val exact = enabled.filter { rule -> rule.senderIds.any { sender.equals(it, ignoreCase = true) } }
        if (exact.isNotEmpty()) return exact
        return enabled.filter { rule ->
            rule.senderIds.any {
                sender.contains(it, ignoreCase = true) || it.contains(sender, ignoreCase = true)
            }
        }
    }

    private fun BankRule.detectType(message: String): TransactionType? {
        val debitIndex = debitKeywords.minOfOrNull { message.indexOf(it, ignoreCase = true).positiveOrMax() }
            ?: Int.MAX_VALUE
        val creditIndex = creditKeywords.minOfOrNull { message.indexOf(it, ignoreCase = true).positiveOrMax() }
            ?: Int.MAX_VALUE
        return when {
            debitIndex == Int.MAX_VALUE && creditIndex == Int.MAX_VALUE -> null
            debitIndex <= creditIndex -> TransactionType.WITHDRAWAL
            else -> TransactionType.DEPOSIT
        }
    }

    private fun CategoryRule.matches(description: String): Boolean =
        keywords.any { description.contains(it, ignoreCase = true) }

    private fun Int.positiveOrMax(): Int = if (this >= 0) this else Int.MAX_VALUE

    private fun extract(
        message: String,
        patterns: List<String>,
        token: String,
        capture: String,
    ): String? = patterns.firstNotNullOfOrNull { pattern ->
        compile(pattern, token, capture)?.find(message)?.groupValues?.getOrNull(1)
    }

    private fun compile(pattern: String, token: String, capture: String): Regex? {
        val index = pattern.indexOf(token)
        if (index < 0) return null
        val before = Regex.escape(pattern.substring(0, index))
        val afterText = pattern.substring(index + token.length)
        val after = Regex.escape(afterText)
        return Regex(before + capture + after, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    }

    companion object {
        const val REASON_TYPE_NOT_FOUND = "type_not_found"
        const val REASON_AMOUNT_NOT_FOUND = "amount_not_found"
        const val REASON_DESCRIPTION_NOT_FOUND = "description_not_found"
        private const val AMOUNT_TOKEN = "{amount}"
        private const val DESCRIPTION_TOKEN = "{description}"
        private const val REFERENCE_TOKEN = "{ref}"
        private const val AMOUNT_CAPTURE = "([\\d,]+(?:\\.\\d+)?)"
        private const val DESCRIPTION_CAPTURE = "(.+?)"
        private const val REFERENCE_CAPTURE = "(\\w{6,})"
    }
}

@Singleton
class RuleBasedSmsParserFactory @Inject constructor() : SmsParserFactory {
    override fun create(bankRules: List<BankRule>, categoryRules: List<CategoryRule>): SmsParser =
        RuleBasedSmsParser(bankRules, categoryRules)
}
