package com.usagecontrol.core.model

data class RuleAction(
    val id: Long = 0,
    val type: ActionType,
    val params: Map<String, String> = emptyMap(),
)

enum class ActionType {
    // Notification actions
    DISMISS_NOTIFICATION,   // auto-dismiss
    MUTE_NOTIFICATION,      // remove sound/vibration but keep
    SNOOZE_NOTIFICATION,    // params: "durationMinutes" -> "30"

    // Screen time actions
    ADVISE,                 // show notification warning
    SOFT_BLOCK,             // overlay with "continue anyway"
    HARD_BLOCK,             // overlay with no bypass

    // Lock/Sound actions
    SET_VOLUME,             // params: "stream" -> "alarm|ring|notification|media", "level" -> "0-100"
    SET_RINGER_MODE,        // params: "mode" -> "silent|vibrate|normal"
    SET_DND,                // params: "enabled" -> "true|false"
    SET_LOCK_TIMEOUT,       // params: "timeoutMs" -> "30000"
}
