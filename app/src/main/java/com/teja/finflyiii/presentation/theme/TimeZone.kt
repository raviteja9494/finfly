/* Presentation-level timezone selected by the persisted display preference. */
package com.teja.finflyiii.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.ZoneId

val LocalFinFlyIIIZoneId = staticCompositionLocalOf { ZoneId.systemDefault() }
