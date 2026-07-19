/* Domain use case converting a parsed SMS preview into a Firefly transaction. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.ParsedTransaction
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.TransactionDraft
import com.teja.finflyiii.domain.model.TransactionType
import com.teja.finflyiii.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

/** Persists one already-reviewed parsed transaction through the normal repository path. */
class SubmitParsedTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(parsed: ParsedTransaction): Result<Transaction> {
        val withdrawal = parsed.type == TransactionType.WITHDRAWAL
        return repository.saveTransaction(
            TransactionDraft(
                type = parsed.type,
                amount = BigDecimal.valueOf(parsed.amount),
                description = parsed.description,
                date = Instant.ofEpochMilli(parsed.timestamp),
                sourceAccountId = if (withdrawal) parsed.fireflyAccountId.takeIf(String::isNotBlank) else null,
                sourceAccount = if (withdrawal) parsed.accountName else parsed.description,
                destinationAccountId = if (!withdrawal) parsed.fireflyAccountId.takeIf(String::isNotBlank) else null,
                destinationAccount = if (withdrawal) parsed.description else parsed.accountName,
                category = parsed.category,
                tags = parsed.tags,
                notes = buildList {
                    if (parsed.reference.isNotBlank()) add("Ref: ${parsed.reference}")
                    add("Raw SMS: ${parsed.rawSms}")
                }.joinToString("\n"),
            )
        )
    }
}
