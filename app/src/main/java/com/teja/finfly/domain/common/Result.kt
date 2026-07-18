/* Domain-layer success and failure wrapper used across all repository boundaries. */
package com.teja.finfly.domain.common

/** Represents either a successful [value] or a user-safe failure [message]. */
sealed interface Result<out T> {
    /** A completed operation containing [value]. */
    data class Success<T>(val value: T) : Result<T>

    /** A failed operation containing display-safe context and an optional diagnostic cause. */
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>
}

/** Transforms a successful value while retaining failure context. */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(value))
    is Result.Error -> this
}
