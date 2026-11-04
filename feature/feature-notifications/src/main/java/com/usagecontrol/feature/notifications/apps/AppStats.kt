package com.usagecontrol.feature.notifications.apps

import com.usagecontrol.core.database.dao.AppEventRow
import com.usagecontrol.core.database.dao.ChannelCountRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pure per-app statistics for the Apps tab and app profile. Day and hour buckets use local time. */
object AppStats {

    const val UNKNOWN_CHANNEL = "Unknown channel"

    /** Average alerts per day over a [days]-day window; 0 for a non-positive window. */
    fun alertsPerDay(total: Int, days: Int): Double =
        if (days <= 0) 0.0 else total.toDouble() / days

    /**
     * Counts per local calendar day for the [days] days ending today (inclusive), oldest first.
     * Timestamps outside the window are ignored.
     */
    fun dailySeries(timestamps: List<Long>, days: Int, zone: ZoneId, now: Long): List<Int> {
        if (days <= 0) return emptyList()
        val today = localDate(now, zone)
        val first = today.minusDays(days - 1L)
        val series = IntArray(days)
        for (ts in timestamps) {
            val index = (localDate(ts, zone).toEpochDay() - first.toEpochDay()).toInt()
            if (index in 0 until days) series[index]++
        }
        return series.toList()
    }

    /**
     * Same window as [dailySeries], built from SQL day buckets keyed "yyyy-MM-dd"
     * (NotificationDao.appDailyCountsFlow). Unparseable keys are ignored.
     */
    fun dailySeriesFromDayCounts(dayCounts: Map<String, Int>, days: Int, today: LocalDate): List<Int> {
        if (days <= 0) return emptyList()
        val first = today.minusDays(days - 1L)
        return (0 until days).map { dayCounts[first.plusDays(it.toLong()).toString()] ?: 0 }
    }

    /** Fraction of alerts the user dismissed, 0..1; 0 when there are no alerts. */
    fun dismissRate(dismissed: Int, total: Int): Float =
        if (total <= 0) 0f else (dismissed.toFloat() / total).coerceIn(0f, 1f)

    /**
     * Median ms from post to the user's action. Rows without an actionTimestamp are skipped, as are
     * negative gaps (the row was re-posted after the action). Null when no row qualifies.
     */
    fun medianTimeToAct(events: List<AppEventRow>): Long? {
        val gaps = events.mapNotNull { e ->
            e.actionTimestamp?.let { it - e.timestamp }?.takeIf { it >= 0 }
        }.sorted()
        if (gaps.isEmpty()) return null
        val mid = gaps.size / 2
        return if (gaps.size % 2 == 1) gaps[mid] else (gaps[mid - 1] + gaps[mid]) / 2
    }

    /** Alerts per local hour of day; always 24 entries, index 0 = midnight. */
    fun hourlyHistogram(timestamps: List<Long>, zone: ZoneId): List<Int> {
        val hours = IntArray(24)
        for (ts in timestamps) hours[Instant.ofEpochMilli(ts).atZone(zone).hour]++
        return hours.toList()
    }

    /** Channel label to count, busiest first. NULL or blank channel ids merge into [UNKNOWN_CHANNEL]. */
    fun channelBreakdown(rows: List<ChannelCountRow>): List<Pair<String, Int>> =
        rows.groupBy { it.channelId?.takeIf(String::isNotBlank) ?: UNKNOWN_CHANNEL }
            .map { (label, group) -> label to group.sumOf { it.count } }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

    private fun localDate(ts: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
}
