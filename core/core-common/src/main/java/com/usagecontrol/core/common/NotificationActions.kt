package com.usagecontrol.core.common

/**
 * Phone-side actions on posted notifications, performed through the notification listener.
 * Lives in core-common so feature modules can act on notifications without depending on
 * service modules; the implementation is bound by service-notification.
 */
interface NotificationActions {

    /** Outcome of [archive] and [snooze]. */
    enum class Result {
        /** The listener accepted the request. */
        DONE,

        /** The listener is not bound (access revoked or service not yet connected). */
        LISTENER_UNAVAILABLE,

        /** The listener is bound but the system rejected the call. */
        FAILED,
    }

    /** Outcome of [open]. */
    enum class OpenResult {
        /** The notification's own content intent was sent. */
        OPENED,

        /** The content intent was gone, so the app's launcher activity was started instead. */
        FALLBACK_LAUNCHED,

        /** Neither the content intent nor a launch intent could be used. */
        UNAVAILABLE,
    }

    /** Whether the notification listener is currently bound. */
    val isListenerConnected: Boolean

    /** Removes the notification with [key] from the shade. */
    fun archive(key: String): Result

    /** Snoozes the notification with [key] for [durationMs]; the system reposts it afterwards. */
    fun snooze(key: String, durationMs: Long): Result

    /** Opens the original notification target, falling back to launching [packageName]. */
    fun open(key: String?, packageName: String): OpenResult
}
