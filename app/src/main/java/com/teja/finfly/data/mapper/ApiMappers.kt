/* Data-layer mappings from Firefly API resources into Room cache entities. */
package com.teja.finfly.data.mapper

import com.teja.finfly.data.local.entity.AccountEntity
import com.teja.finfly.data.local.entity.CategoryEntity
import com.teja.finfly.data.local.entity.TransactionEntity
import com.teja.finfly.data.network.dto.AccountResource
import com.teja.finfly.data.network.dto.CategoryResource
import com.teja.finfly.data.network.dto.TransactionResource
import com.teja.finfly.domain.model.TransactionType
import java.time.Instant
import java.time.OffsetDateTime

fun TransactionResource.toEntities(): List<TransactionEntity> = attributes.transactions.mapIndexed { index, split ->
    val type = when (split.type.lowercase()) {
        "deposit" -> TransactionType.DEPOSIT
        "transfer" -> TransactionType.TRANSFER
        else -> TransactionType.WITHDRAWAL
    }
    val account = when (type) {
        TransactionType.DEPOSIT -> split.destinationName ?: split.sourceName
        else -> split.sourceName ?: split.destinationName
    }.orEmpty()
    TransactionEntity(
        id = "$id-$index",
        amount = split.amount,
        description = split.description,
        category = split.categoryName.orEmpty(),
        account = account,
        dateEpochMillis = parseInstant(split.date).toEpochMilli(),
        type = type.name,
        tags = split.tags.joinToString(TAG_SEPARATOR),
        notes = split.notes,
        rawSms = null,
        currency = split.currencyCode ?: "XXX",
    )
}

fun AccountResource.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = attributes.name,
    balance = attributes.currentBalance ?: "0",
    currency = attributes.currencyCode ?: "XXX",
    type = attributes.type,
)

fun CategoryResource.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = attributes.name,
    color = "",
    icon = "",
)

private fun parseInstant(value: String): Instant = runCatching { Instant.parse(value) }
    .recoverCatching { OffsetDateTime.parse(value).toInstant() }
    .getOrDefault(Instant.EPOCH)
