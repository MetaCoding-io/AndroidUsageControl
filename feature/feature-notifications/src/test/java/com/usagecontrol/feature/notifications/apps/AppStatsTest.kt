package com.usagecontrol.feature.notifications.apps

import com.usagecontrol.core.database.dao.AppEventRow
import com.usagecontrol.core.database.dao.ChannelCountRow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStatsTest {

    private val zone = ZoneId.of("America/New_York")
    private val now = at(2026, 11, 4, 15, 0)

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun event(timestamp: Long, actionTimestamp: Long? = null) =
        AppEventRow(timestamp = timestamp, actionTimestamp = actionTimestamp, removedAt = null, channelId = null)

    @Test
    fun alertsPerDay_averagesAndGuardsZeroWindow() {
        assertEquals(3.0, AppStats.alertsPerDay(21, 7), 0.0001)
        assertEquals(0.0, AppStats.alertsPerDay(0, 7), 0.0001)
        assertEquals(0.0, AppStats.alertsPerDay(5, 0), 0.0001)
        assertEquals(0.0, AppStats.alertsPerDay(5, -1), 0.0001)
    }

    @Test
    fun dailySeries_bucketsByLocalDay_acrossMidnight() {
        val ts = listOf(
            at(2026, 11, 3, 23, 59), // yesterday local, but already Nov 4 in UTC
            at(2026, 11, 4, 0, 0),
            at(2026, 11, 4, 0, 1),
            at(2026, 10, 29, 12, 0), // first day of a 7-day window
            at(2026, 10, 28, 23, 59), // just outside
            at(2026, 11, 5, 0, 1), // tomorrow ignored
        )
        val series = AppStats.dailySeries(ts, days = 7, zone = zone, now = now)
        assertEquals(listOf(1, 0, 0, 0, 0, 1, 2), series)
    }

    @Test
    fun dailySeries_emptyAndNonPositiveWindow() {
        assertEquals(List(7) { 0 }, AppStats.dailySeries(emptyList(), 7, zone, now))
        assertTrue(AppStats.dailySeries(listOf(now), 0, zone, now).isEmpty())
    }

    @Test
    fun dailySeries_spansDstFallBack() {
        // US DST ended 2026-11-01; the 25-hour day must still be one bucket.
        val ts = listOf(at(2026, 11, 1, 0, 30), at(2026, 11, 1, 23, 30))
        val series = AppStats.dailySeries(ts, days = 4, zone = zone, now = now)
        assertEquals(listOf(2, 0, 0, 0), series)
    }

    @Test
    fun dailySeriesFromDayCounts_alignsKeysAndFillsGaps() {
        val counts = mapOf("2026-11-04" to 5, "2026-11-02" to 2, "2026-10-01" to 9, "garbage" to 1)
        val series = AppStats.dailySeriesFromDayCounts(counts, days = 3, today = LocalDate.of(2026, 11, 4))
        assertEquals(listOf(2, 0, 5), series)
        assertTrue(AppStats.dailySeriesFromDayCounts(counts, 0, LocalDate.of(2026, 11, 4)).isEmpty())
    }

    @Test
    fun dismissRate_fractionClampedAndZeroTotal() {
        assertEquals(0.25f, AppStats.dismissRate(1, 4), 0.0001f)
        assertEquals(0f, AppStats.dismissRate(0, 0), 0.0001f)
        assertEquals(1f, AppStats.dismissRate(5, 4), 0.0001f)
    }

    @Test
    fun medianTimeToAct_skipsNullAndNegative_oddAndEven() {
        val odd = listOf(event(0, 100), event(0, 300), event(0, 200), event(0, null))
        assertEquals(200L, AppStats.medianTimeToAct(odd))
        val even = listOf(event(0, 100), event(0, 400), event(1_000, 500))
        assertEquals(250L, AppStats.medianTimeToAct(even))
    }

    @Test
    fun medianTimeToAct_nullWhenNothingQualifies() {
        assertNull(AppStats.medianTimeToAct(emptyList()))
        assertNull(AppStats.medianTimeToAct(listOf(event(0, null), event(1_000, 10))))
    }

    @Test
    fun hourlyHistogram_always24_localHours() {
        assertEquals(List(24) { 0 }, AppStats.hourlyHistogram(emptyList(), zone))
        val hist = AppStats.hourlyHistogram(
            listOf(at(2026, 11, 3, 23, 59), at(2026, 11, 4, 0, 0), at(2026, 11, 4, 0, 30), now),
            zone,
        )
        assertEquals(24, hist.size)
        assertEquals(1, hist[23])
        assertEquals(2, hist[0])
        assertEquals(1, hist[15])
        assertEquals(4, hist.sum())
    }

    @Test
    fun channelBreakdown_mapsNullAndBlankToUnknown_sortedDesc() {
        val rows = listOf(
            ChannelCountRow("messages", 3),
            ChannelCountRow(null, 2),
            ChannelCountRow("", 2),
            ChannelCountRow("promo", 3),
        )
        assertEquals(
            listOf(AppStats.UNKNOWN_CHANNEL to 4, "messages" to 3, "promo" to 3),
            AppStats.channelBreakdown(rows),
        )
        assertTrue(AppStats.channelBreakdown(emptyList()).isEmpty())
    }
}
