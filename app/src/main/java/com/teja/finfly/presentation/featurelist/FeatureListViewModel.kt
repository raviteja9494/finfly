/* Presentation-layer ViewModel for a secondary Firefly feature list. */
package com.teja.finfly.presentation.featurelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.FireflyFeature
import com.teja.finfly.domain.repository.FireflyFeatureRepository
import com.teja.finfly.presentation.navigation.AppRoute
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
}
