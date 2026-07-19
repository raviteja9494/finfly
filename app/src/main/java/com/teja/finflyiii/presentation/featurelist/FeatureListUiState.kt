/* Presentation-layer state contract for secondary Firefly feature lists. */
package com.teja.finflyiii.presentation.featurelist

import com.teja.finflyiii.domain.model.FireflyFeatureItem

/** Loading, content, empty, and failure states for a drawer feature destination. */
sealed interface FeatureListUiState {
    data object Loading : FeatureListUiState
    data class Success(val items: List<FireflyFeatureItem>) : FeatureListUiState
    data object Empty : FeatureListUiState
    data object Error : FeatureListUiState
}

data class FeatureDeletionState(
    val deletingId: String? = null,
    val failed: Boolean = false,
)
