/* Presentation-layer pills shared by transaction lists and detail screens. */
package com.teja.finfly.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teja.finfly.R
import com.teja.finfly.presentation.theme.FinFlyThemeTokens

/** Renders a subtle reusable tag pill using semantic theme colors. */
@Composable
fun TagPill(tag: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(FinFlyThemeTokens.radii.chip),
        ).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
        )
    }
}

/** Renders the transaction category as a quiet secondary pill. */
@Composable
fun CategoryPill(category: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(FinFlyThemeTokens.radii.chip),
        ).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = category.ifBlank { stringResource(R.string.category_uncategorized) },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}
