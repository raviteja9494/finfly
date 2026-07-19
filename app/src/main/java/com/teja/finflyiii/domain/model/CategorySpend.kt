/* Domain-layer category spending bucket used by Dashboard analytics. */
package com.teja.finflyiii.domain.model

import java.math.BigDecimal

/** Aggregated withdrawal amount for a transaction category. */
data class CategorySpend(val category: String, val amount: BigDecimal)
