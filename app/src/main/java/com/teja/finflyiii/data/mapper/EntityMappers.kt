/* Data-layer mappings between Room entities and pure domain models. */
package com.teja.finflyiii.data.mapper

import com.teja.finflyiii.data.local.entity.AccountEntity
import com.teja.finflyiii.data.local.entity.TransactionEntity
import com.teja.finflyiii.data.local.entity.CategoryEntity
import com.teja.finflyiii.data.local.entity.TagEntity
import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.Tag
import com.teja.finflyiii.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    remoteGroupId = remoteGroupId,
    journalId = journalId,
    amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    description = description,
    category = category,
    budget = budget,
    account = account,
    sourceAccountId = sourceAccountId,
    sourceAccount = sourceAccount,
    destinationAccountId = destinationAccountId,
    destinationAccount = destinationAccount,
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

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    color = color,
    icon = icon,
)

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name)

internal const val TAG_SEPARATOR = "\u001F"
