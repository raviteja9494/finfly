/* Domain repository contract for secondary Firefly list features. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.FireflyFeature
import com.teja.finfly.domain.model.FireflyFeatureItem
import com.teja.finfly.domain.model.FireflyFeatureDraft

/** Loads one lightweight list for a secondary drawer destination. */
interface FireflyFeatureRepository {
    suspend fun load(feature: FireflyFeature): Result<List<FireflyFeatureItem>>
    suspend fun create(draft: FireflyFeatureDraft): Result<FireflyFeatureItem>
}
