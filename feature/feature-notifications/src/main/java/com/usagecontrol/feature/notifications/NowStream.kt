package com.usagecontrol.feature.notifications

import com.usagecontrol.core.model.NotificationRecord
import com.usagecontrol.core.model.ProvisionalLane
import com.usagecontrol.core.ui.theme.Lane
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/*
 * Pure grouping logic for the Now stream: lane mapping, time sections, bundles and search.
 * No Android or Compose runtime dependencies so it runs as plain JVM unit tests.
 */

private const val HOUR_MS = 60 * 60 * 1000L

/**
 * Maps a capture-time lane onto the six UI lanes. Rows without a lane (legacy v3 rows or
 * unknown names) are treated as ordinary updates.
 */
fun toLane(lane: ProvisionalLane?): Lane = when (lane) {
    ProvisionalLane.PRIORITY -> Lane.Live
    ProvisionalLane.PEOPLE -> Lane.People
    ProvisionalLane.UPDATES, null -> Lane.Work
    ProvisionalLane.QUIET -> Lane.Noise
}

/** The persisted provisional lane of this row, null when missing or unrecognised. */
val NotificationRecord.provisional: ProvisionalLane?
    get() = ProvisionalLane.fromName(provisionalLane)

/** UI lane for this row. */
val NotificationRecord.lane: Lane
    get() = toLane(provisional)

/** Time sections of the Now stream, in display order. */
enum class NowSection { NOW, EARLIER_TODAY, OLDER }

/**
 * Splits [rows] into time sections relative to [now] in [zone]: [NowSection.NOW] for the
 * last hour (and any future-dated rows), [NowSection.EARLIER_TODAY] for the rest of the
 * local day, [NowSection.OLDER] for yesterday and before. Empty sections are omitted; row
 * order within a section is preserved.
 */
fun sectionize(
    rows: List<NotificationRecord>,
    now: Long,
    zone: ZoneId,
): List<Pair<NowSection, List<NotificationRecord>>> {
    val startOfToday = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        .atStartOfDay(zone).toInstant().toEpochMilli()
    val grouped = rows.groupBy { row ->
        when {
            now - row.timestamp < HOUR_MS -> NowSection.NOW
            row.timestamp >= startOfToday -> NowSection.EARLIER_TODAY
            else -> NowSection.OLDER
        }
    }
    return NowSection.entries.mapNotNull { section -> grouped[section]?.let { section to it } }
}

/** One entry of a rendered section: a single notification or a bundle of several. */
sealed interface StreamItem {
    /** Stable identity for list keys. */
    val id: String

    /** Newest timestamp in the item. */
    val timestamp: Long
}

data class SingleItem(val record: NotificationRecord) : StreamItem {
    override val id: String get() = "n:${record.id}"
    override val timestamp: Long get() = record.timestamp
}

/**
 * Several rows from one app collapsed together, either sharing a groupKey or sharing a
 * low-priority lane (UPDATES/QUIET). [rows] keep the input order, newest first when the
 * input is newest first.
 */
data class BundleItem(
    val packageName: String,
    val appName: String,
    val groupKey: String?,
    val lane: Lane,
    val rows: List<NotificationRecord>,
) : StreamItem {
    override val id: String
        get() = if (groupKey != null) "bg:$packageName:$groupKey" else "bl:$packageName:${lane.name}"
    override val timestamp: Long get() = rows.maxOf { it.timestamp }
    val count: Int get() = rows.size
}

/**
 * Collapses [rows] into stream items. Rows in the UPDATES or QUIET lane bundle per app and
 * lane; other rows bundle by package + groupKey. A candidate bundle with fewer than two rows
 * stays a [SingleItem]. Items are ordered by the position of their first row in [rows].
 */
fun bundle(rows: List<NotificationRecord>): List<StreamItem> {
    val buckets = LinkedHashMap<Any, MutableList<NotificationRecord>>()
    rows.forEachIndexed { index, row ->
        buckets.getOrPut(bundleKey(row) ?: index) { mutableListOf() }.add(row)
    }
    return buckets.map { (key, group) ->
        if (key is BundleKey && group.size >= 2) {
            val first = group.first()
            BundleItem(
                packageName = first.packageName,
                appName = first.appName,
                groupKey = key.groupKey,
                lane = first.lane,
                rows = group,
            )
        } else {
            // Unbundled rows have a unique Int key, so the group holds exactly one row.
            SingleItem(group.single())
        }
    }
}

private data class BundleKey(val packageName: String, val groupKey: String?, val lane: ProvisionalLane?)

private fun bundleKey(row: NotificationRecord): BundleKey? {
    val lane = row.provisional
    return when {
        lane == ProvisionalLane.UPDATES || lane == ProvisionalLane.QUIET ->
            BundleKey(row.packageName, groupKey = null, lane = lane)
        !row.groupKey.isNullOrEmpty() -> BundleKey(row.packageName, row.groupKey, lane = null)
        else -> null
    }
}

/**
 * Case-insensitive search over title, text and app name. A blank [query] returns [rows]
 * unchanged.
 */
fun filter(rows: List<NotificationRecord>, query: String): List<NotificationRecord> {
    val q = query.trim()
    if (q.isEmpty()) return rows
    return rows.filter { row ->
        row.title?.contains(q, ignoreCase = true) == true ||
            row.text?.contains(q, ignoreCase = true) == true ||
            row.appName.contains(q, ignoreCase = true)
    }
}

/**
 * Milliseconds from [now] until the next occurrence of [hour]:[minute] in [zone]: later today
 * when that time is still ahead, otherwise tomorrow. A time equal to now resolves to
 * tomorrow, so the result is always positive. DST gaps resolve the way [ZonedDateTime]
 * does (shifted forward by the gap length).
 *
 * @throws IllegalArgumentException when [hour] or [minute] is out of range.
 */
fun snoozeDurationUntil(hour: Int, minute: Int, now: Long, zone: ZoneId): Long {
    require(hour in 0..23) { "hour out of range: $hour" }
    require(minute in 0..59) { "minute out of range: $minute" }
    val current = Instant.ofEpochMilli(now).atZone(zone)
    val time = LocalTime.of(hour, minute)
    var target = ZonedDateTime.of(current.toLocalDate(), time, zone)
    if (!target.isAfter(current)) {
        target = ZonedDateTime.of(current.toLocalDate().plusDays(1), time, zone)
    }
    return target.toInstant().toEpochMilli() - now
}
