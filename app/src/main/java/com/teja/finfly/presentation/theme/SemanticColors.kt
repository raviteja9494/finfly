/* Presentation-layer semantic transaction colors derived from the active theme. */
package com.teja.finfly.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val ColorScheme.creditAmount: Color
    @Composable get() = if (isLight) Color(0xFF167A5B) else Color(0xFF80D4B0)

val ColorScheme.debitAmount: Color
    @Composable get() = if (isLight) Color(0xFFB43C57) else Color(0xFFFF8FA8)

private val ColorScheme.isLight: Boolean
    get() = background.luminance() > 0.5f
