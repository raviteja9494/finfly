/* Pure domain representation of a financial transaction. */
package com.teja.finfly.domain.model

import java.math.BigDecimal
import java.time.Instant

/** A normalized ledger entry consumed by FinFly business rules and presentation. */
data class Transaction(
    val id: String,
    val remoteGroupId: String,
    val journalId: String,
    val amount: BigDecimal,
    val description: String,
    val category: String,
    val account: String,
    val sourceAccountId: String?,
    val sourceAccount: String,
    val destinationAccountId: String?,
    val destinationAccount: String,
    val date: Instant,
    val type: TransactionType,
    val tags: List<String>,
    val notes: String?,
    val rawSms: String?,
    val currency: String,
)
