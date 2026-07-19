/* Data-layer Room entity for cached Firefly categories. */
package com.teja.finflyiii.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val icon: String,
)
