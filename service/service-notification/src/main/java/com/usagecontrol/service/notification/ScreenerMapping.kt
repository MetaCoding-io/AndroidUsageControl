package com.usagecontrol.service.notification

import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.RouteKind

/** The sender a Screener prompt routes: a person within a conversation app, or the whole app. */
data class ScreenerTarget(val kind: RouteKind, val packageName: String, val key: String) {
    /** Stable identity used to prompt each sender only once. */
    val dedupeKey: String get() = "${kind.name}|$packageName|$key"
}

/**
 * Pure Screener decisions for [NotificationCaptureService] and [ScreenerNotifier];
 * JVM-testable without a device.
 */
object ScreenerMapping {

    /** Notification channel for Screener prompts (Doorman's own channel). */
    const val CHANNEL_ID = "screener"

    /** Android shows at most three notification actions. */
    const val MAX_ACTIONS = 3

    private const val NOTIFICATION_ID_BASE = 0x5C00_0000
    private const val NOTIFICATION_ID_RANGE = 1_000_000

    /**
     * Conversations with a person key are screened per person (PERSON route); everything
     * else per app (APP route, key "").
     */
    fun targetFor(packageName: String, isConversation: Boolean, personKey: String?): ScreenerTarget =
        if (isConversation && !personKey.isNullOrEmpty()) {
            ScreenerTarget(RouteKind.PERSON, packageName, personKey)
        } else {
            ScreenerTarget(RouteKind.APP, packageName, "")
        }

    /**
     * A Screener prompt is only posted for live, unrouted posts from other apps: never
     * for Doorman's own notifications (that would screen the Screener), group summaries,
     * or the reconnect reconcile pass (which must not burst prompts).
     */
    fun shouldScreen(
        needsScreening: Boolean,
        packageName: String,
        ownPackage: String,
        isGroupSummary: Boolean,
        interactive: Boolean,
    ): Boolean = needsScreening && interactive && !isGroupSummary && packageName != ownPackage

    /** Doorman's own Screener prompts are not history; the listener skips them entirely. */
    fun isOwnScreenerPost(packageName: String, channelId: String?, ownPackage: String): Boolean =
        packageName == ownPackage && channelId == CHANNEL_ID

    /**
     * Lanes offered as one-tap actions: the lane the notification was provisionally filed
     * in first, then the rest by sortOrder, capped at [max].
     */
    fun actionLanes(lanes: List<Lane>, suggestedLaneId: Long, max: Int = MAX_ACTIONS): List<Lane> {
        if (max <= 0) return emptyList()
        val sorted = lanes.sortedWith(compareBy({ it.sortOrder }, { it.id }))
        val suggested = sorted.firstOrNull { it.id == suggestedLaneId }
        return (listOfNotNull(suggested) + sorted.filter { it.id != suggestedLaneId }).take(max)
    }

    /** Stable, non-negative notification id per target so a re-prompt replaces, not stacks. */
    fun notificationIdFor(target: ScreenerTarget): Int =
        NOTIFICATION_ID_BASE + Math.floorMod(target.dedupeKey.hashCode(), NOTIFICATION_ID_RANGE)
}
