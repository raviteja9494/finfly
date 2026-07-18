/* Presentation-layer Phase 3 placeholder that intentionally contains no charts. */
package com.teja.finfly.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teja.finfly.R
import com.teja.finfly.presentation.components.ErrorState
import com.teja.finfly.presentation.components.LoadingState

@Composable
fun ReportsScreen(state: ReportsUiState = ReportsUiState.Empty) {
    when (state) {
        ReportsUiState.Loading -> LoadingState()
        ReportsUiState.Error -> ErrorState()
        ReportsUiState.Empty, ReportsUiState.Success -> ReportsPlaceholder()
    }
}

@Composable
private fun ReportsPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp))
        Text(
            stringResource(R.string.reports_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
