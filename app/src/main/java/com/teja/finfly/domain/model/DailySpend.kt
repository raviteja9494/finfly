/* Domain-layer daily spending bucket used by the dashboard chart. */
package com.teja.finfly.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/** Total withdrawals for one local calendar day. */
data class DailySpend(val date: LocalDate, val amount: BigDecimal)
