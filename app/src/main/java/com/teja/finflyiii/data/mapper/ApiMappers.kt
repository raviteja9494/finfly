/* Data-layer mappings from Firefly API resources into Room cache entities. */
package com.teja.finflyiii.data.mapper

import com.teja.finflyiii.data.local.entity.AccountEntity
import com.teja.finflyiii.data.local.entity.CategoryEntity
import com.teja.finflyiii.data.local.entity.TransactionEntity
import com.teja.finflyiii.data.local.entity.TagEntity
import com.teja.finflyiii.data.network.dto.AccountResource
import com.teja.finflyiii.data.network.dto.CategoryResource
import com.teja.finflyiii.data.network.dto.TransactionResource
import com.teja.finflyiii.data.network.dto.TagResource
import com.teja.finflyiii.domain.model.TransactionType
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
        remoteGroupId = id,
        journalId = split.transactionJournalId,
        amount = split.amount,
        description = split.description,
        category = split.categoryName.orEmpty(),
        budget = split.budgetName.orEmpty(),
        account = account,
        sourceAccountId = split.sourceId,
        sourceAccount = split.sourceName.orEmpty(),
        destinationAccountId = split.destinationId,
        destinationAccount = split.destinationName.orEmpty(),
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

fun TagResource.toEntity(): TagEntity = TagEntity(id = id, name = attributes.tag)

private fun parseInstant(value: String): Instant = runCatching { Instant.parse(value) }
    .recoverCatching { OffsetDateTime.parse(value).toInstant() }
    .getOrDefault(Instant.EPOCH)
