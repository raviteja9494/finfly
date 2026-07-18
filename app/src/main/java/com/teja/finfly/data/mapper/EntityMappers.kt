/* Data-layer mappings between Room entities and pure domain models. */
package com.teja.finfly.data.mapper

import com.teja.finfly.data.local.entity.AccountEntity
import com.teja.finfly.data.local.entity.TransactionEntity
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    description = description,
    category = category,
    account = account,
    date = Instant.ofEpochMilli(dateEpochMillis),
    type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.WITHDRAWAL),
    tags = tags.split(TAG_SEPARATOR).filter(String::isNotBlank),
    notes = notes,
    rawSms = rawSms,
    currency = currency,
)

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    balance = balance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    currency = currency,
    type = type,
)

internal const val TAG_SEPARATOR = "\u001F"
