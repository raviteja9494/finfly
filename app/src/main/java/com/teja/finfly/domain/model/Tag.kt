/* Pure domain representation of a Firefly transaction tag. */
package com.teja.finfly.domain.model

/** A reusable Firefly tag available to transaction filters and editors. */
data class Tag(val id: String, val name: String)
