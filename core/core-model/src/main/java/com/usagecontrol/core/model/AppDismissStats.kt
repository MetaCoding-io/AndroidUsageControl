package com.usagecontrol.core.model

data class AppDismissStats(
    val packageName: String,
    val appName: String,
    val totalNotifications: Int,
    val dismissedCount: Int,
    val openedCount: Int,
    val autoDismissedCount: Int,
    val avgTimeToActionMs: Long?,
) {
    val dismissRate: Float
        get() = if (totalNotifications > 0) dismissedCount.toFloat() / totalNotifications else 0f

    val openRate: Float
        get() = if (totalNotifications > 0) openedCount.toFloat() / totalNotifications else 0f
}
