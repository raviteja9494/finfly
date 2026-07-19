/* Domain use case that converts cached finance data into a bounded model prompt. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.AiConfig
import com.teja.finflyiii.domain.model.FinanceContext
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.TransactionFilter
import com.teja.finflyiii.domain.model.TransactionType
import com.teja.finflyiii.domain.repository.AccountRepository
import com.teja.finflyiii.domain.repository.SmsRulesRepository
import com.teja.finflyiii.domain.repository.TransactionRepository
import java.time.Clock
import java.time.ZoneId
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Builds a private prompt exclusively from FinFly III's existing Room-backed repository streams. */
class FinanceContextBuilder @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val smsRulesRepository: SmsRulesRepository,
    private val clock: Clock,
) {
    suspend fun buildContext(config: AiConfig, question: String = ""): FinanceContext {
        val now = clock.instant()
        val zone = clock.zone
        val period = resolvePeriod(question, now, zone, config.dateRangeDays)
        val transactions = transactionRepository.observeTransactions(
            filter = TransactionFilter(from = period.from, until = period.until),
            limit = config.maxTransactions,
            offset = 0,
        ).first().successOrEmpty()
        val accounts = if (config.includeBalances) {
            accountRepository.observeAccounts().first().successOrEmpty()
        } else emptyList()
        val smsRuleSummary = if (config.includeSmsRules) loadSmsRuleSummary() else null

        val prompt = buildString {
            appendLine("FINFLY PRIVATE FINANCE CONTEXT")
            appendLine("Current date: ${now.atZone(zone).toLocalDate()}")
            appendLine("Included period: ${period.from.atZone(zone).toLocalDate()} through ${period.displayUntil}")
            if (config.includeBalances) appendAccounts(accounts)
            if (config.includeCategories) appendCategoryTotals(transactions)
            appendTransactions(transactions)
            if (smsRuleSummary != null) appendSmsRules(smsRuleSummary)
            appendLine("Only transactions inside the included period are present. Say when cached data is insufficient.")
        }
        val wasTruncated = prompt.length > MAX_CONTEXT_CHARACTERS
        val boundedPrompt = if (wasTruncated) prompt.take(MAX_CONTEXT_CHARACTERS) + TRUNCATION_MARKER else prompt
        return FinanceContext(
            prompt = boundedPrompt,
            estimatedTokens = estimateTokens(boundedPrompt),
            transactionCount = transactions.size,
            dateRangeDays = period.dayCount,
            wasTruncated = wasTruncated,
        )
    }

    private fun StringBuilder.appendAccounts(accounts: List<Account>) {
        appendLine("\nACCOUNTS")
        if (accounts.isEmpty()) appendLine("No cached accounts.")
        accounts.forEach { appendLine("- ${it.name}: ${it.balance.toPlainString()} ${it.currency} (${it.type})") }
    }

    private fun StringBuilder.appendCategoryTotals(transactions: List<Transaction>) {
        appendLine("\nCATEGORY TOTALS")
        val totals = transactions.filter {
            it.type == TransactionType.WITHDRAWAL && it.category.isNotBlank()
        }
            .groupBy { it.category to it.currency }
            .mapValues { (_, values) -> values.fold(java.math.BigDecimal.ZERO) { sum, item -> sum + item.amount.abs() } }
            .entries.sortedByDescending { it.value }
        if (totals.isEmpty()) appendLine("No categorized transactions.")
        totals.forEach { (key, total) -> appendLine("- ${key.first}: ${total.toPlainString()} ${key.second}") }
    }

    private fun StringBuilder.appendTransactions(transactions: List<Transaction>) {
        appendLine("\nRECENT TRANSACTIONS")
        if (transactions.isEmpty()) appendLine("No cached transactions in this period.")
        transactions.forEach { transaction ->
            val date = DATE_FORMAT.format(transaction.date.atZone(ZoneId.systemDefault()))
            val kind = when (transaction.type) {
                TransactionType.WITHDRAWAL -> "expense"
                TransactionType.DEPOSIT -> "income"
                TransactionType.TRANSFER -> "transfer"
            }
            append("- $date | $kind | ")
            append("${transaction.amount.abs().toPlainString()} ${transaction.currency} | ${transaction.description}")
            if (transaction.category.isNotBlank()) append(" | category=${transaction.category}")
            if (transaction.budget.isNotBlank()) append(" | budget=${transaction.budget}")
            if (transaction.tags.isNotEmpty()) append(" | tags=${transaction.tags.joinToString()}")
            appendLine()
        }
    }

    private suspend fun loadSmsRuleSummary(): SmsRuleSummary = SmsRuleSummary(
        bankNames = smsRulesRepository.getBankRules().successOrEmpty().filter { it.enabled }.map { it.name },
        categoryNames = smsRulesRepository.getCategoryRules().successOrEmpty().filter { it.enabled }.map { it.name },
        universalTags = smsRulesRepository.getUniversalTags().successOrEmpty(),
    )

    private fun StringBuilder.appendSmsRules(summary: SmsRuleSummary) {
        appendLine("\nPARSING RULES")
        appendLine("- Bank rules: ${summary.bankNames.joinToString()}")
        appendLine("- Category rules: ${summary.categoryNames.joinToString()}")
        appendLine("- Universal tags: ${summary.universalTags.joinToString()}")
    }

    private fun <T> Result<List<T>>.successOrEmpty(): List<T> = when (this) {
        is Result.Success -> value
        is Result.Error -> emptyList()
    }

    companion object {
        private const val MAX_CONTEXT_CHARACTERS = 4_000
        private const val TRUNCATION_MARKER = "\n[Context truncated to fit the on-device model.]"
        private val DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE

        /** Fast conservative estimate suitable for displaying prompt size without loading the model. */
        fun estimateTokens(text: String): Int = (text.length + 3) / 4

        internal fun resolvePeriod(
            question: String,
            now: Instant,
            zone: ZoneId,
            defaultDays: Int,
        ): ContextPeriod {
            val normalized = question.lowercase()
            val today = now.atZone(zone).toLocalDate()
            val (fromDate, untilDate) = when {
                "last month" in normalized -> today.withDayOfMonth(1).minusMonths(1) to today.withDayOfMonth(1)
                "this month" in normalized || "current month" in normalized ->
                    today.withDayOfMonth(1) to today.plusDays(1)
                "today" in normalized -> today to today.plusDays(1)
                else -> return ContextPeriod(
                    from = now.minus(defaultDays.toLong(), ChronoUnit.DAYS),
                    until = now,
                    displayUntil = today,
                    dayCount = defaultDays,
                )
            }
            return ContextPeriod(
                from = fromDate.atStartOfDay(zone).toInstant(),
                until = untilDate.atStartOfDay(zone).toInstant().coerceAtMost(now),
                displayUntil = untilDate.minusDays(1).coerceAtMost(today),
                dayCount = ChronoUnit.DAYS.between(fromDate, untilDate).toInt().coerceAtLeast(1),
            )
        }
    }

    internal data class ContextPeriod(
        val from: Instant,
        val until: Instant,
        val displayUntil: LocalDate,
        val dayCount: Int,
    )

    private data class SmsRuleSummary(
        val bankNames: List<String>,
        val categoryNames: List<String>,
        val universalTags: List<String>,
    )
}
