/* Domain use case that converts cached finance data into a bounded model prompt. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.AiConfig
import com.teja.finfly.domain.model.FinanceContext
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.SmsRulesRepository
import com.teja.finfly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Builds a private prompt exclusively from FinFly's existing Room-backed repository streams. */
class FinanceContextBuilder @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val smsRulesRepository: SmsRulesRepository,
    private val clock: Clock,
) {
    suspend fun buildContext(config: AiConfig): FinanceContext {
        val now = clock.instant()
        val from = now.minus(config.dateRangeDays.toLong(), ChronoUnit.DAYS)
        val transactions = transactionRepository.observeTransactions(
            filter = TransactionFilter(from = from, until = now),
            limit = config.maxTransactions,
            offset = 0,
        ).first().successOrEmpty()
        val accounts = if (config.includeBalances) {
            accountRepository.observeAccounts().first().successOrEmpty()
        } else emptyList()
        val smsRuleSummary = if (config.includeSmsRules) loadSmsRuleSummary() else null

        val prompt = buildString {
            appendLine("FINFLY PRIVATE FINANCE CONTEXT")
            appendLine("Current date: ${now.atZone(ZoneId.systemDefault()).toLocalDate()}")
            appendLine("Period: last ${config.dateRangeDays} days")
            if (config.includeBalances) appendAccounts(accounts)
            if (config.includeCategories) appendCategoryTotals(transactions)
            appendTransactions(transactions)
            if (smsRuleSummary != null) appendSmsRules(smsRuleSummary)
            appendLine("Use only this context for account-specific facts. Say when the cached data is insufficient.")
        }
        val wasTruncated = prompt.length > MAX_CONTEXT_CHARACTERS
        val boundedPrompt = if (wasTruncated) prompt.take(MAX_CONTEXT_CHARACTERS) + TRUNCATION_MARKER else prompt
        return FinanceContext(
            prompt = boundedPrompt,
            estimatedTokens = estimateTokens(boundedPrompt),
            transactionCount = transactions.size,
            dateRangeDays = config.dateRangeDays,
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
        val totals = transactions.filter { it.category.isNotBlank() }
            .groupBy(Transaction::category)
            .mapValues { (_, values) -> values.fold(java.math.BigDecimal.ZERO) { sum, item -> sum + item.amount.abs() } }
            .entries.sortedByDescending { it.value }
        if (totals.isEmpty()) appendLine("No categorized transactions.")
        totals.forEach { (category, total) -> appendLine("- $category: ${total.toPlainString()}") }
    }

    private fun StringBuilder.appendTransactions(transactions: List<Transaction>) {
        appendLine("\nRECENT TRANSACTIONS")
        if (transactions.isEmpty()) appendLine("No cached transactions in this period.")
        transactions.forEach { transaction ->
            val date = DATE_FORMAT.format(transaction.date.atZone(ZoneId.systemDefault()))
            append("- $date | ${transaction.type.name.lowercase()} | ")
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
    }

    private data class SmsRuleSummary(
        val bankNames: List<String>,
        val categoryNames: List<String>,
        val universalTags: List<String>,
    )
}
