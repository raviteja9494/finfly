/* Domain repository contract for secondary Firefly list features. */
package com.teja.finflyiii.domain.repository

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.FireflyFeature
import com.teja.finflyiii.domain.model.FireflyFeatureItem
import com.teja.finflyiii.domain.model.FireflyFeatureDraft
import com.teja.finflyiii.domain.model.FireflyRuleGroup

/** Loads one lightweight list for a secondary drawer destination. */
interface FireflyFeatureRepository {
    suspend fun load(feature: FireflyFeature): Result<List<FireflyFeatureItem>>
    suspend fun create(draft: FireflyFeatureDraft): Result<FireflyFeatureItem>
    suspend fun loadDraft(feature: FireflyFeature, id: String): Result<FireflyFeatureDraft>
    suspend fun update(id: String, draft: FireflyFeatureDraft): Result<FireflyFeatureItem>
    suspend fun loadRuleGroups(): Result<List<FireflyRuleGroup>>
    suspend fun delete(feature: FireflyFeature, id: String): Result<Unit>
}
