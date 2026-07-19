/* Data-layer Room entity for cached Firefly accounts. */
package com.teja.finflyiii.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: String,
    val currency: String,
    val type: String,
)
