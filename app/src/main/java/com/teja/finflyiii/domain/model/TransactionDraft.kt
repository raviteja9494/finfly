/* Pure domain input model for creating or updating a Firefly transaction. */
package com.teja.finflyiii.domain.model

import java.math.BigDecimal
import java.time.Instant

/** User-editable values required to create or update one Firefly transaction split. */
data class TransactionDraft(
    val localId: String? = null,
    val remoteGroupId: String? = null,
    val journalId: String? = null,
    val type: TransactionType = TransactionType.WITHDRAWAL,
    val amount: BigDecimal = BigDecimal.ZERO,
    val description: String = "",
    val date: Instant,
    val sourceAccountId: String? = null,
    val sourceAccount: String = "",
    val destinationAccountId: String? = null,
    val destinationAccount: String = "",
    val category: String = "",
    val budget: String = "",
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val currency: String = "",
)
