package com.usagecontrol.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index("packageName"),
        Index("timestamp"),
        Index("userAction"),
        Index(value = ["key"], unique = true),
        Index("laneId"),
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
    // v4: StatusBarNotification identity; legacy (v3) rows keep NULL
    val key: String? = null,
    val notifId: Int? = null,
    val tag: String? = null,
    val channelId: String? = null,
    @ColumnInfo(defaultValue = "0")
    val importance: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isConversation: Boolean = false,
    val shortcutId: String? = null,
    @ColumnInfo(defaultValue = "0")
    val whenMs: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val isGroupSummary: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val flags: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val updateCount: Int = 0,
    val removalReason: String? = null, // REASON_* mapped name, e.g. CLICK, CANCEL
    val removedAt: Long? = null,
    val smallIconPath: String? = null,
    val largeIconPath: String? = null,
    @ColumnInfo(defaultValue = "0")
    val color: Int = 0,
    val provisionalLane: String? = null,
    val messagesJson: String? = null,
    val actionsJson: String? = null,
    // v5: resolved Lane id (lanes.id); NULL until routed
    val laneId: Long? = null,
)
