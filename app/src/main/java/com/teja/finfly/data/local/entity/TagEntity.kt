/* Data-layer Room entity for cached Firefly transaction tags. */
package com.teja.finfly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
)
