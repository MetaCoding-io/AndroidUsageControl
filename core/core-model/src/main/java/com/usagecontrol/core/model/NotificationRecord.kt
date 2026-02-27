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
)

enum class NotificationAction {
    NONE,
    DISMISSED,
    OPENED,
    SNOOZED,
    AUTO_DISMISSED,
}
