/* Pure domain input model for creating a Firefly account. */
package com.teja.finfly.domain.model

import java.math.BigDecimal
import java.time.Instant

/** User-editable values accepted by the basic Firefly account creation flow. */
data class AccountDraft(
    val id: String? = null,
    val name: String,
    val type: String,
    val currency: String,
    val openingBalance: BigDecimal?,
    val openingBalanceDate: Instant?,
)
