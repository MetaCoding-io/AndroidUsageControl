package com.usagecontrol.core.model

/**
 * Lane assigned to a notification at capture time, before any user triage.
 * Persisted by [name] in the notification history row.
 */
enum class ProvisionalLane {
    /** Time-sensitive: alarms, reminders, high-importance channels. */
    PRIORITY,

    /** Conversations, messages and calls from people. */
    PEOPLE,

    /** Default lane for ordinary app updates. */
    UPDATES,

    /** Promotions, social, recommendations and low-importance noise. */
    QUIET;

    companion object {
        /** Parses a persisted lane name; unknown or null values yield null. */
        fun fromName(name: String?): ProvisionalLane? = entries.firstOrNull { it.name == name }
    }
}
