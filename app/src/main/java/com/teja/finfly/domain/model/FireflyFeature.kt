/* Pure domain models for secondary Firefly feature lists. */
package com.teja.finfly.domain.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/** Secondary Firefly sections exposed from the application drawer. */
@Serializable
enum class FireflyFeature { BUDGETS, CATEGORIES, TAGS, BILLS, PIGGY_BANKS }

/** Display-ready summary of one secondary Firefly resource. */
data class FireflyFeatureItem(
    val id: String,
    val title: String,
    val details: List<String> = emptyList(),
    val progressPercent: Int? = null,
)

/** Validated domain inputs accepted by Firefly feature creation. */
sealed interface FireflyFeatureDraft {
    val name: String
    val notes: String

    data class Budget(
        override val name: String,
        override val notes: String,
        val monthlyAmount: BigDecimal? = null,
        val currencyCode: String = "",
    ) : FireflyFeatureDraft

    data class Category(
        override val name: String,
        override val notes: String,
    ) : FireflyFeatureDraft

    data class Tag(
        override val name: String,
        override val notes: String,
    ) : FireflyFeatureDraft

    data class Bill(
        override val name: String,
        override val notes: String,
        val minimumAmount: BigDecimal,
        val maximumAmount: BigDecimal,
        val currencyCode: String,
        val firstDueDate: LocalDate,
        val repeatFrequency: String,
    ) : FireflyFeatureDraft

    data class PiggyBank(
        override val name: String,
        override val notes: String,
        val accountId: String,
        val targetAmount: BigDecimal,
        val currentAmount: BigDecimal? = null,
        val startDate: LocalDate,
        val targetDate: LocalDate? = null,
    ) : FireflyFeatureDraft
}
