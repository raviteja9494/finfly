/* Presentation-layer serializable route definitions for type-safe Compose Navigation. */
package com.teja.finfly.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable data object Dashboard : AppRoute
    @Serializable data object Transactions : AppRoute
    @Serializable data object Reports : AppRoute
    @Serializable data object Settings : AppRoute
}
