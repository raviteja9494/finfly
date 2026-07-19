/* Pure domain models for secondary Firefly feature lists. */
package com.teja.finflyiii.domain.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/** Secondary Firefly sections exposed from the application drawer. */
@Serializable
enum class FireflyFeature { BUDGETS, CATEGORIES, TAGS, BILLS, PIGGY_BANKS, RULES }

/** Display-ready summary of one secondary Firefly resource. */
data class FireflyFeatureItem(
    val id: String,
    val title: String,
    val details: List<String> = emptyList(),
    val progressPercent: Int? = null,
    val setAmount: BigDecimal? = null,
    val spentAmount: BigDecimal? = null,
    val currencyCode: String = "",
)

/** One editable trigger or action belonging to a Firefly rule. */
data class FireflyRuleClause(
    val id: String? = null,
    val type: String,
    val value: String = "",
    val active: Boolean = true,
    val prohibited: Boolean = false,
    val stopProcessing: Boolean = false,
)

/** A Firefly rule group offered by the rule editor. */
data class FireflyRuleGroup(val id: String, val title: String)

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

    data class Rule(
        override val name: String,
        override val notes: String,
        val groupId: String,
        val active: Boolean = true,
        val strict: Boolean = true,
        val stopProcessing: Boolean = false,
        val triggers: List<FireflyRuleClause>,
        val actions: List<FireflyRuleClause>,
    ) : FireflyFeatureDraft
}
