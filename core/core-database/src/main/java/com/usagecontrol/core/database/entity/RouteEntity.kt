package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One route per (kind, packageName, key); key is "" for APP routes so UNIQUE holds. */
@Entity(
    tableName = "routes",
    indices = [
        Index(value = ["kind", "packageName", "key"], unique = true),
        Index("laneId"),
    ]
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: String, // RouteKind name
    val packageName: String,
    val key: String,
    val laneId: Long,
    val createdAt: Long,
)
