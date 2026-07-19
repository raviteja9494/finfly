/* Presentation-layer composition locals for reusable FinFly III spacing and shape tokens. */
package com.teja.finflyiii.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FinFlyIIISpacing(
    val xSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val xLarge: Dp = 32.dp,
)

@Immutable
data class FinFlyIIIRadii(
    val chip: Dp = 10.dp,
    val card: Dp = 24.dp,
    val hero: Dp = 32.dp,
)

internal val LocalFinFlyIIISpacing = staticCompositionLocalOf { FinFlyIIISpacing() }
internal val LocalFinFlyIIIRadii = staticCompositionLocalOf { FinFlyIIIRadii() }

object FinFlyIIIThemeTokens {
    val spacing: FinFlyIIISpacing
        @Composable get() = LocalFinFlyIIISpacing.current
    val radii: FinFlyIIIRadii
        @Composable get() = LocalFinFlyIIIRadii.current
}
