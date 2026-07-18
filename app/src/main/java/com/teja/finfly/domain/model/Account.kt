/* Pure domain representation of a Firefly III account. */
package com.teja.finfly.domain.model

import java.math.BigDecimal

/** A balance-bearing account displayed and cached by FinFly. */
data class Account(
    val id: String,
    val name: String,
    val balance: BigDecimal,
    val currency: String,
    val type: String,
) {
    /** True for balance-bearing accounts users normally regard as bank or cash accounts. */
    val isBalanceAccount: Boolean
        get() = type.lowercase().let { value ->
            value == "asset" || value == "cash" || value == "liability" || value == "liabilities" ||
                value.contains("asset account") || value.contains("liability")
        }
}
