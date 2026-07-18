/* Pure domain representation of a transaction category. */
package com.teja.finfly.domain.model

/** A Firefly category with portable color and icon identifiers. */
data class Category(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
)
