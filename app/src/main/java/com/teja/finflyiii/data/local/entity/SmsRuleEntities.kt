/* Data-layer Room rows storing each friendly rule as human-readable JSON. */
package com.teja.finflyiii.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_rules")
data class BankRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    val configJson: String,
    val updatedAt: Long,
)

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    val priority: Int,
    val configJson: String,
)

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val result: String,
    val reason: String,
    val matchedRule: String,
    val processedAt: Long,
)
