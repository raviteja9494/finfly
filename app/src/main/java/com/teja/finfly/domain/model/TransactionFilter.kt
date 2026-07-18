/* Pure domain criteria for filtering the cached transaction timeline. */
package com.teja.finfly.domain.model

import java.time.Instant

/** Optional criteria combined with AND semantics when filtering transactions. */
data class TransactionFilter(
    val query: String = "",
    val types: Set<TransactionType> = emptySet(),
    val categories: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val from: Instant? = null,
    val until: Instant? = null,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || types.isNotEmpty() || categories.isNotEmpty() ||
            tags.isNotEmpty() || accountIds.isNotEmpty() || from != null || until != null
}
