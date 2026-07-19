package com.teja.finfly.domain.common

import java.util.Currency

/** Returns true for blank optional values or a real ISO 4217 currency code. */
fun String.isBlankOrIsoCurrencyCode(): Boolean = isBlank() || runCatching {
    length == ISO_CODE_LENGTH && Currency.getInstance(uppercase()).currencyCode == uppercase()
}.getOrDefault(false)

private const val ISO_CODE_LENGTH = 3
