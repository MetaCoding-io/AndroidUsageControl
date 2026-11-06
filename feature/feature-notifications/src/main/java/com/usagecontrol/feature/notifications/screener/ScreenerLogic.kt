package com.usagecontrol.feature.notifications.screener

import com.usagecontrol.core.database.dao.UnroutedRow
import com.usagecontrol.core.model.Route
import com.usagecontrol.core.model.RouteKind

/** A sender no route claims yet, with its newest notification as the sample. */
data class UnscreenedSender(
    val kind: RouteKind,
    val packageName: String,
    /** "" for APP senders, the person key for PERSON senders. */
    val key: String,
    val appName: String,
    val sampleTitle: String?,
    val sampleText: String?,
    val lastTimestamp: Long,
    val count: Int,
    /** Lane the newest notification was provisionally filed in; null for pre-v5 rows. */
    val currentLaneId: Long?,
) {
    val id: String get() = "${kind.name}|$packageName|$key"

    /** "Alice · WhatsApp" for people, the app name otherwise. */
    val displayName: String get() = if (kind == RouteKind.PERSON) "$key · $appName" else appName
}

/** Pure Screener sheet decisions; JVM-testable. */
object ScreenerLogic {

    /**
     * Sender granularity for a row. Mirrors service-notification ScreenerMapping.targetFor:
     * conversations with a person key are screened per person, everything else per app.
     */
    fun senderOf(row: UnroutedRow): Pair<RouteKind, String> {
        val personKey = Route.personKeyOf(row.shortcutId, row.title)
        return if (row.isConversation && !personKey.isNullOrEmpty()) RouteKind.PERSON to personKey
        else RouteKind.APP to ""
    }

    /**
     * Groups unrouted rows (newest first, as [com.usagecontrol.core.database.dao.NotificationDao.unroutedFlow]
     * returns them) into senders, most recently active first. Senders of [focusPackage]
     * (the app whose Screener notification was tapped) move to the top.
     */
    fun unscreenedSenders(rows: List<UnroutedRow>, focusPackage: String? = null): List<UnscreenedSender> {
        val senders = LinkedHashMap<String, UnscreenedSender>()
        for (row in rows.sortedByDescending { it.timestamp }) {
            val (kind, key) = senderOf(row)
            val id = "${kind.name}|${row.packageName}|$key"
            val existing = senders[id]
            senders[id] = existing?.copy(count = existing.count + 1) ?: UnscreenedSender(
                kind = kind,
                packageName = row.packageName,
                key = key,
                appName = row.appName.ifBlank { row.packageName },
                sampleTitle = row.title,
                sampleText = row.text,
                lastTimestamp = row.timestamp,
                count = 1,
                currentLaneId = row.laneId,
            )
        }
        val list = senders.values.toList()
        if (focusPackage.isNullOrEmpty()) return list
        val (focused, rest) = list.partition { it.packageName == focusPackage }
        return focused + rest
    }
}
