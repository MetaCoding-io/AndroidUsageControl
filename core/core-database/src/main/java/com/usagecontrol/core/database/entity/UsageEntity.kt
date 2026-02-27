package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_records",
    indices = [
        Index("packageName"),
        Index("date"),
        Index(value = ["packageName", "date"], unique = true),
    ]
)
data class UsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val date: String, // yyyy-MM-dd
    val foregroundTimeMs: Long,
    val launchCount: Int,
    val firstUsedTimestamp: Long,
    val lastUsedTimestamp: Long,
)
