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
        if (isCasualQuestion(question)) return buildCasualContext()
        val now = clock.instant()
        val zone = clock.zone
        val period = resolvePeriod(question, now, zone, config.dateRangeDays)
        val allTransactions = transactionRepository.observeTransactions(
            filter = TransactionFilter(from = period.from, until = period.until),
            limit = MAX_AGGREGATE_TRANSACTIONS,
            offset = 0,
        ).first().successOrEmpty()
        val detailTransactions = selectDetailTransactions(allTransactions, question, config.maxTransactions)
        val includeBalances = config.includeBalances && question.containsAny(BALANCE_QUERY_TERMS)
        val includeCategories = config.includeCategories && question.containsAny(CATEGORY_QUERY_TERMS)
        val includeSmsRules = config.includeSmsRules && question.containsAny(RULE_QUERY_TERMS)
        val accounts = if (includeBalances) {
            accountRepository.observeAccounts().first().successOrEmpty()
        } else emptyList()
        val smsRuleSummary = if (includeSmsRules) loadSmsRuleSummary() else null

        val prompt = buildString {
            appendLine("FINFLY PRIVATE FINANCE CONTEXT")
            appendLine("Current date: ${now.atZone(zone).toLocalDate()}")
            appendLine("Included period: ${period.from.atZone(zone).toLocalDate()} through ${period.displayUntil}")
            if (includeBalances) appendAccounts(accounts)
            appendLine("Cached transactions in period: ${allTransactions.size}")
            appendExpenseTotals(allTransactions)
            if (includeCategories) appendCategoryTotals(allTransactions)
            appendTransactions(detailTransactions)
            if (smsRuleSummary != null) appendSmsRules(smsRuleSummary)
            appendLine("Only transactions inside the included period are present. Say when cached data is insufficient.")
        }
        val wasTruncated = prompt.length > MAX_CONTEXT_CHARACTERS
        val boundedPrompt = if (wasTruncated) prompt.take(MAX_CONTEXT_CHARACTERS) + TRUNCATION_MARKER else prompt
        return FinanceContext(
            prompt = boundedPrompt,
            estimatedTokens = estimateTokens(boundedPrompt),
            transactionCount = allTransactions.size,
            dateRangeDays = period.dayCount,
            wasTruncated = wasTruncated,
            requiresTransactions = true,
        )
    }

    private fun selectDetailTransactions(
        transactions: List<Transaction>,
        question: String,
        limit: Int,
    ): List<Transaction> {
        val keywords = question.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= MIN_KEYWORD_LENGTH && it !in QUERY_STOP_WORDS }
        if (keywords.isEmpty()) return transactions.take(limit)
        val matches = transactions.filter { transaction ->
            val searchable = buildString {
                append(transaction.description)
                append(' ')
                append(transaction.category)
                append(' ')
                append(transaction.budget)
                append(' ')
                append(transaction.account)
                append(' ')
                append(transaction.tags.joinToString(" "))
            }.lowercase()
            keywords.any(searchable::contains)
        }
        return (matches.ifEmpty { transactions }).take(limit)
    }

    private fun buildCasualContext(): FinanceContext {
        val prompt = buildString {
            appendLine("CASUAL REQUEST")
            appendLine("No financial data was requested.")
            appendLine("Reply in one short sentence and offer help with spending, balances, categories, or budgets.")
            appendLine("Do not list transactions, amounts, internal labels, or cached context.")
        }
        return FinanceContext(
            prompt = prompt,
            estimatedTokens = estimateTokens(prompt),
            transactionCount = 0,
            dateRangeDays = 0,
            wasTruncated = false,
            requiresTransactions = false,
        )
    }

    private fun StringBuilder.appendAccounts(accounts: List<Account>) {
        appendLine("\nACCOUNTS")
        if (accounts.isEmpty()) appendLine("No cached accounts.")
        accounts.take(MAX_ACCOUNT_LINES).forEach {
            appendLine("- ${it.name.promptSafe(MAX_FIELD_CHARACTERS)}: ${it.balance.toPlainString()} ${it.currency} (${it.type})")
        }
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
        totals.take(MAX_CATEGORY_LINES).forEach { (key, total) ->
            appendLine("- ${key.first.promptSafe(MAX_FIELD_CHARACTERS)}: ${total.toPlainString()} ${key.second}")
        }
    }

    private fun StringBuilder.appendExpenseTotals(transactions: List<Transaction>) {
        appendLine("\nEXPENSE TOTALS")
        val totals = transactions
            .filter { it.type == TransactionType.WITHDRAWAL }
            .groupBy(Transaction::currency)
            .mapValues { (_, values) ->
                values.fold(java.math.BigDecimal.ZERO) { sum, item -> sum + item.amount.abs() }
            }
        if (totals.isEmpty()) appendLine("No expenses in this period.")
        totals.toSortedMap().forEach { (currency, total) ->
            appendLine("- $currency: ${total.toPlainString()}")
        }
    }

    private fun StringBuilder.appendTransactions(transactions: List<Transaction>) {
        appendLine("\nTRANSACTION DETAILS")
        if (transactions.isEmpty()) appendLine("No cached transactions in this period.")
        transactions.forEach { transaction ->
            val date = DATE_FORMAT.format(transaction.date.atZone(ZoneId.systemDefault()))
            val kind = when (transaction.type) {
                TransactionType.WITHDRAWAL -> "expense"
                TransactionType.DEPOSIT -> "income"
                TransactionType.TRANSFER -> "transfer"
            }
            append("- $date | $kind | ")
            append("${transaction.amount.abs().toPlainString()} ${transaction.currency} | ${transaction.description.promptSafe(MAX_DESCRIPTION_CHARACTERS)}")
            if (transaction.category.isNotBlank()) append(" | category=${transaction.category.promptSafe(MAX_FIELD_CHARACTERS)}")
            if (transaction.budget.isNotBlank()) append(" | budget=${transaction.budget.promptSafe(MAX_FIELD_CHARACTERS)}")
            if (transaction.tags.isNotEmpty()) {
                append(" | tags=${transaction.tags.take(MAX_TAGS_PER_TRANSACTION).joinToString { it.promptSafe(MAX_FIELD_CHARACTERS) }}")
            }
            appendLine()
        }
    }

    private fun String.promptSafe(maxCharacters: Int): String =
        replace(Regex("[\\r\\n]+"), " ").trim().take(maxCharacters)

    private fun String.containsAny(terms: Set<String>): Boolean {
        val normalized = lowercase()
        return terms.any(normalized::contains)
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
        private const val MAX_CONTEXT_CHARACTERS = 2_200
        private const val MAX_AGGREGATE_TRANSACTIONS = 10_000
        private const val MIN_KEYWORD_LENGTH = 3
        private const val MAX_ACCOUNT_LINES = 20
        private const val MAX_CATEGORY_LINES = 12
        private const val MAX_TAGS_PER_TRANSACTION = 5
        private const val MAX_DESCRIPTION_CHARACTERS = 80
        private const val MAX_FIELD_CHARACTERS = 40
        private const val TRUNCATION_MARKER = "\n[Context truncated to fit the on-device model.]"
        private val DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE
        private val CASUAL_QUESTIONS = setOf(
            "hi",
            "hello",
            "hey",
            "good morning",
            "good afternoon",
            "good evening",
            "thanks",
            "thank you",
            "help",
        )
        private val QUERY_STOP_WORDS = setOf(
            "and", "are", "balance", "did", "expense", "expenses", "for", "from", "how", "last",
            "month", "much", "show", "spend", "spent", "summarize", "summary", "the", "this", "today",
            "transaction", "transactions", "was", "what", "yesterday",
        )
        private val BALANCE_QUERY_TERMS = setOf(
            "account", "asset", "balance", "liabilit", "net worth",
        )
        private val CATEGORY_QUERY_TERMS = setOf(
            "categor", "breakdown", "food", "summar", "top spend",
        )
        private val RULE_QUERY_TERMS = setOf(
            "bank rule", "category rule", "parsing", "sms rule",
        )

        /** Fast conservative estimate suitable for displaying prompt size without loading the model. */
        fun estimateTokens(text: String): Int = (text.length + 3) / 4

        internal fun isCasualQuestion(question: String): Boolean {
            val normalized = question.trim().lowercase().replace(Regex("[^a-z ]"), "")
            return normalized in CASUAL_QUESTIONS
        }

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
