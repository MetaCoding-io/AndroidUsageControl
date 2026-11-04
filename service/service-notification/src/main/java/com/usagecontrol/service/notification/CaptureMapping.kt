package com.usagecontrol.service.notification

import android.app.Notification
import android.service.notification.NotificationListenerService

/**
 * Pure mapping helpers for [NotificationCaptureService]. They only touch compile-time
 * constants, so they are JVM-testable without a device.
 */
object CaptureMapping {

    const val MEDIA_STYLE_TEMPLATE = "android.app.Notification\$MediaStyle"
    const val DECORATED_MEDIA_STYLE_TEMPLATE = "android.app.Notification\$DecoratedMediaCustomViewStyle"

    private const val LIVE_FLAGS = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE

    /**
     * True when a notification belongs in the live table (ongoing, foreground service, or
     * media) rather than history.
     */
    fun isLive(flags: Int, template: String?, hasMediaSession: Boolean): Boolean =
        (flags and LIVE_FLAGS) != 0 ||
            hasMediaSession ||
            template == MEDIA_STYLE_TEMPLATE ||
            template == DECORATED_MEDIA_STYLE_TEMPLATE

    /** Stable string for a NotificationListenerService.REASON_* code; unknown codes become OTHER_<n>. */
    fun removalReasonName(reason: Int): String = when (reason) {
        NotificationListenerService.REASON_CLICK -> "CLICK"
        NotificationListenerService.REASON_CANCEL -> "CANCEL"
        NotificationListenerService.REASON_CANCEL_ALL -> "CANCEL_ALL"
        NotificationListenerService.REASON_APP_CANCEL -> "APP_CANCEL"
        NotificationListenerService.REASON_APP_CANCEL_ALL -> "APP_CANCEL_ALL"
        NotificationListenerService.REASON_LISTENER_CANCEL -> "LISTENER_CANCEL"
        NotificationListenerService.REASON_LISTENER_CANCEL_ALL -> "LISTENER_CANCEL_ALL"
        NotificationListenerService.REASON_SNOOZED -> "SNOOZED"
        NotificationListenerService.REASON_TIMEOUT -> "TIMEOUT"
        NotificationListenerService.REASON_GROUP_SUMMARY_CANCELED -> "GROUP_SUMMARY_CANCELED"
        NotificationListenerService.REASON_CHANNEL_BANNED -> "CHANNEL_BANNED"
        NotificationListenerService.REASON_PACKAGE_BANNED -> "PACKAGE_BANNED"
        NotificationListenerService.REASON_PACKAGE_SUSPENDED -> "PACKAGE_SUSPENDED"
        NotificationListenerService.REASON_USER_STOPPED -> "USER_STOPPED"
        NotificationListenerService.REASON_ERROR -> "ERROR"
        else -> "OTHER_$reason"
    }
}
