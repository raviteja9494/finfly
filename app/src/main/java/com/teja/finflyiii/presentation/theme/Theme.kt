/* Presentation-layer light and dark Material 3 theme provider for FinFly III. */
package com.teja.finflyiii.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Iris,
    onPrimary = Night,
    primaryContainer = NightSurfaceHigh,
    onPrimaryContainer = TextDark,
    secondary = Rose,
    onSecondary = Night,
    secondaryContainer = Color(0xFF49303D),
    onSecondaryContainer = Rose,
    tertiary = Foam,
    onTertiary = Night,
    background = Night,
    onBackground = TextDark,
    surface = NightSurface,
    onSurface = TextDark,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark,
    error = Love,
)

private val LightColors = lightColorScheme(
    primary = IrisDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDF7),
    onPrimaryContainer = TextLight,
    secondary = RoseDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E0E3),
    onSecondaryContainer = TextLight,
    tertiary = Pine,
    onTertiary = Color.White,
    background = Dawn,
    onBackground = TextLight,
    surface = DawnSurface,
    onSurface = TextLight,
    surfaceVariant = DawnSurfaceHigh,
    onSurfaceVariant = MutedLight,
    outline = OutlineLight,
    error = Color(0xFFBA1A1A),
)

@Composable
fun FinFlyIIITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFinFlyIIISpacing provides FinFlyIIISpacing(),
        LocalFinFlyIIIRadii provides FinFlyIIIRadii(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = FinFlyIIITypography,
            content = content,
        )
    }
}
