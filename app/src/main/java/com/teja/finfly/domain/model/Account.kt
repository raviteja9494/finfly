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
    /** Normalized account family used by grouping and balance summaries. */
    val group: AccountGroup
        get() = type.lowercase().let { value ->
            when {
                value.contains("liabil") -> AccountGroup.LIABILITY
                value == "revenue" || value.contains("revenue account") -> AccountGroup.REVENUE
                value == "expense" || value.contains("expense account") -> AccountGroup.EXPENSE
                else -> AccountGroup.ASSET
            }
        }

    /** True for balance-bearing accounts users normally regard as bank or cash accounts. */
    val isBalanceAccount: Boolean
        get() = type.lowercase().let { value ->
            value == "asset" || value == "cash" || value == "liability" || value == "liabilities" ||
                value.contains("asset account") || value.contains("liability")
        }
}

/** User-facing account families supported by Firefly III. */
enum class AccountGroup { ASSET, LIABILITY, REVENUE, EXPENSE }
