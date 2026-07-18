/* Pure domain models for secondary Firefly feature lists. */
package com.teja.finfly.domain.model

import kotlinx.serialization.Serializable

/** Secondary Firefly sections exposed from the application drawer. */
@Serializable
enum class FireflyFeature { BUDGETS, CATEGORIES, BILLS, PIGGY_BANKS }

/** Display-ready summary of one secondary Firefly resource. */
data class FireflyFeatureItem(
    val id: String,
    val title: String,
    val details: List<String> = emptyList(),
    val progressPercent: Int? = null,
)
