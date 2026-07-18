/* Presentation-layer composition locals for reusable FinFly spacing and shape tokens. */
package com.teja.finfly.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FinFlySpacing(
    val xSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val xLarge: Dp = 32.dp,
)

@Immutable
data class FinFlyRadii(
    val chip: Dp = 10.dp,
    val card: Dp = 24.dp,
    val hero: Dp = 32.dp,
)

internal val LocalFinFlySpacing = staticCompositionLocalOf { FinFlySpacing() }
internal val LocalFinFlyRadii = staticCompositionLocalOf { FinFlyRadii() }

object FinFlyThemeTokens {
    val spacing: FinFlySpacing
        @Composable get() = LocalFinFlySpacing.current
    val radii: FinFlyRadii
        @Composable get() = LocalFinFlyRadii.current
}
