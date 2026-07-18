/* Presentation-layer state contract for secondary Firefly feature lists. */
package com.teja.finfly.presentation.featurelist

import com.teja.finfly.domain.model.FireflyFeatureItem

/** Loading, content, empty, and failure states for a drawer feature destination. */
sealed interface FeatureListUiState {
    data object Loading : FeatureListUiState
    data class Success(val items: List<FireflyFeatureItem>) : FeatureListUiState
    data object Empty : FeatureListUiState
    data object Error : FeatureListUiState
}
