/* Presentation-level timezone selected by the persisted display preference. */
package com.teja.finfly.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.ZoneId

val LocalFinFlyZoneId = staticCompositionLocalOf { ZoneId.systemDefault() }
