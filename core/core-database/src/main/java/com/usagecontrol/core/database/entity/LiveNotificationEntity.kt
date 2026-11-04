package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Currently-posted ongoing / foreground-service / media notifications. These are
 * kept out of the history table and reconciled against activeNotifications on
 * listener reconnect.
 */
@Entity(tableName = "live_notifications")
data class LiveNotificationEntity(
    @PrimaryKey
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val category: String?,
    val channelId: String?,
    val isOngoing: Boolean,
    val postTime: Long,
    val smallIconPath: String?,
    val flags: Int,
)
