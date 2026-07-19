/* Presentation-layer ViewModel for a secondary Firefly feature list. */
package com.teja.finflyiii.presentation.featurelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.FireflyFeature
import com.teja.finflyiii.domain.repository.FireflyFeatureRepository
import com.teja.finflyiii.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Loads and retries the lightweight list selected through the global navigation drawer. */
@HiltViewModel
class FeatureListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FireflyFeatureRepository,
) : ViewModel() {
    val feature: FireflyFeature = savedStateHandle.toRoute<AppRoute.FeatureList>().feature
    private val mutableState = MutableStateFlow<FeatureListUiState>(FeatureListUiState.Loading)
    val uiState = mutableState.asStateFlow()
    private val mutableDeletionState = MutableStateFlow(FeatureDeletionState())
    val deletionState = mutableDeletionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.value = FeatureListUiState.Loading
            mutableState.value = when (val result = repository.load(feature)) {
                is Result.Error -> FeatureListUiState.Error
                is Result.Success -> if (result.value.isEmpty()) FeatureListUiState.Empty
                else FeatureListUiState.Success(result.value)
            }
        }
    }

    fun delete(itemId: String) {
        if (mutableDeletionState.value.deletingId != null) return
        viewModelScope.launch {
            mutableDeletionState.value = FeatureDeletionState(deletingId = itemId)
            when (repository.delete(feature, itemId)) {
                is Result.Error -> mutableDeletionState.value = FeatureDeletionState(failed = true)
                is Result.Success -> {
                    mutableDeletionState.value = FeatureDeletionState()
                    load()
                }
            }
        }
    }

    fun dismissDeleteError() {
        mutableDeletionState.value = FeatureDeletionState()
    }
}
