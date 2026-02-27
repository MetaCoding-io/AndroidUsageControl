package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index("packageName"),
        Index("timestamp"),
        Index("userAction"),
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val subText: String?,
    val category: String?,
    val priority: Int,
    val timestamp: Long,
    val groupKey: String?,
    val isOngoing: Boolean,
    val userAction: String, // NotificationAction name
    val actionTimestamp: Long?,
    val ruleIdIfAutoDismissed: Long?,
)
