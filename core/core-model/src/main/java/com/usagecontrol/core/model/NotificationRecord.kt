package com.usagecontrol.core.model

data class NotificationRecord(
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
    val userAction: NotificationAction = NotificationAction.NONE,
    val actionTimestamp: Long? = null,
    val ruleIdIfAutoDismissed: Long? = null,
    val key: String? = null,
    val notifId: Int? = null,
    val tag: String? = null,
    val channelId: String? = null,
    val importance: Int = 0,
    val isConversation: Boolean = false,
    val shortcutId: String? = null,
    val whenMs: Long = 0,
    val isGroupSummary: Boolean = false,
    val flags: Int = 0,
    val updateCount: Int = 0,
    val removalReason: String? = null,
    val removedAt: Long? = null,
    val smallIconPath: String? = null,
    val largeIconPath: String? = null,
    val color: Int = 0,
    val provisionalLane: String? = null,
    val messagesJson: String? = null,
    val actionsJson: String? = null,
    val laneId: Long? = null,
)

enum class NotificationAction {
    NONE,
    DISMISSED,
    OPENED,
    SNOOZED,
    AUTO_DISMISSED,

    /** Archived from the Now stream by the user (swipe); hidden from Now, undoable for a short window. */
    ARCHIVED,
}
