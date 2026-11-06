package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lanes")
data class LaneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val policy: String, // LanePolicy name
    val decayMinutes: Int,
    val sound: String?,
    val breakthroughsJson: String,
    val isDefault: Boolean,
    val sortOrder: Int,
)
