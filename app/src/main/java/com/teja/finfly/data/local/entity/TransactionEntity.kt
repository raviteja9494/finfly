/* Data-layer Room entity for normalized Firefly transaction splits. */
package com.teja.finfly.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "transactions",
    indices = [Index("dateEpochMillis"), Index("type")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(defaultValue = "''") val remoteGroupId: String,
    @ColumnInfo(defaultValue = "''") val journalId: String,
    val amount: String,
    val description: String,
    val category: String,
    val account: String,
    val sourceAccountId: String?,
    @ColumnInfo(defaultValue = "''") val sourceAccount: String,
    val destinationAccountId: String?,
    @ColumnInfo(defaultValue = "''") val destinationAccount: String,
    val dateEpochMillis: Long,
    val type: String,
    val tags: String,
    val notes: String?,
    val rawSms: String?,
    val currency: String,
)
