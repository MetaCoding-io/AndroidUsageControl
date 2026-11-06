package com.usagecontrol.core.model

/** How a lane delivers notifications filed into it. Persisted by [name]. */
enum class LanePolicy {
    /** Delivered immediately with sound/vibration. */
    Instant,

    /** Held and delivered together at batch times. */
    Batched,

    /** Delivered silently; visible only in the app. */
    Quiet,

    /** Rolled up into a periodic digest. */
    Digest,

    /** Never surfaced; history only. */
    Blocked;

    companion object {
        fun fromName(name: String?): LanePolicy? = entries.firstOrNull { it.name == name }
    }
}

/**
 * A user-defined destination for notifications (R014). Six defaults are seeded
 * ([DefaultLanes]); users can rename/recolour them and add their own.
 */
data class Lane(
    val id: Long = 0,
    val name: String,
    /** ARGB colour, e.g. 0xFFFF6B5A.toInt(). */
    val colorArgb: Int,
    val policy: LanePolicy,
    /** Minutes before an unread notification in this lane decays out of view; 0 = never. */
    val decayMinutes: Int,
    /** Sound URI or preset name; null = lane is silent / system default by policy. */
    val sound: String? = null,
    /** JSON array of breakthrough conditions (people, keywords) that bypass the policy. */
    val breakthroughsJson: String = "[]",
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)

/** What a [Route] matches on, in ascending precedence (PERSON beats CHANNEL beats APP). */
enum class RouteKind {
    APP,
    CHANNEL,
    PERSON;

    companion object {
        fun fromName(name: String?): RouteKind? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Files notifications from an app, one of its channels, or one person into a lane.
 * [key] is "" for APP routes, the channelId for CHANNEL routes and the person key
 * ([personKeyOf]) for PERSON routes.
 */
data class Route(
    val id: Long = 0,
    val kind: RouteKind,
    val packageName: String,
    val key: String = "",
    val laneId: Long,
    val createdAt: Long,
) {
    companion object {
        /**
         * Stable per-app person identity: the conversation shortcut id when present,
         * otherwise the notification title (sender name). Must match the SQL
         * COALESCE(shortcutId, title) used by RouteDao re-filing.
         */
        fun personKeyOf(shortcutId: String?, title: String?): String? =
            shortcutId?.takeIf { it.isNotEmpty() } ?: title?.takeIf { it.isNotEmpty() }
    }
}

/**
 * Seeded default lanes with fixed ids so routing can map classifier output to them
 * without a lookup. Colours mirror the dark LaneColors palette in core-ui
 * (core-model cannot depend on Compose).
 */
object DefaultLanes {
    const val INSTANT_ID = 1L
    const val PEOPLE_ID = 2L
    const val BATCHED_ID = 3L
    const val QUIET_ID = 4L
    const val DIGEST_ID = 5L
    const val BLOCKED_ID = 6L

    val all: List<Lane> = listOf(
        Lane(INSTANT_ID, "Instant", 0xFFB4F03C.toInt(), LanePolicy.Instant, 60, isDefault = true, sortOrder = 0),   // lime
        Lane(PEOPLE_ID, "People", 0xFFFF6B5A.toInt(), LanePolicy.Instant, 240, isDefault = true, sortOrder = 1),    // coral
        Lane(BATCHED_ID, "Batched", 0xFF2ED3E6.toInt(), LanePolicy.Batched, 180, isDefault = true, sortOrder = 2),  // cyan
        Lane(QUIET_ID, "Quiet", 0xFF9B7BFF.toInt(), LanePolicy.Quiet, 720, isDefault = true, sortOrder = 3),        // violet
        Lane(DIGEST_ID, "Digest", 0xFFFFB020.toInt(), LanePolicy.Digest, 1440, isDefault = true, sortOrder = 4),    // amber
        Lane(BLOCKED_ID, "Blocked", 0xFF8A94A6.toInt(), LanePolicy.Blocked, 0, isDefault = true, sortOrder = 5),    // slate
    )

    /** Default lane for a capture-time provisional classification. */
    fun forProvisional(lane: ProvisionalLane?): Long = when (lane) {
        ProvisionalLane.PRIORITY -> INSTANT_ID
        ProvisionalLane.PEOPLE -> PEOPLE_ID
        ProvisionalLane.UPDATES, null -> BATCHED_ID
        ProvisionalLane.QUIET -> QUIET_ID
    }
}
