package com.usagecontrol.feature.notifications

import com.usagecontrol.core.model.NotificationRecord
import com.usagecontrol.core.model.ProvisionalLane
import com.usagecontrol.core.ui.theme.Lane
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NowStreamTest {

    private val zone = ZoneId.of("America/New_York")
    private val now = at(2026, 11, 4, 15, 0)

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun row(
        id: Long,
        timestamp: Long = now,
        pkg: String = "com.example.chat",
        appName: String = "Chat",
        title: String? = "Alice",
        text: String? = "hello",
        groupKey: String? = null,
        lane: String? = ProvisionalLane.PEOPLE.name,
    ) = NotificationRecord(
        id = id,
        packageName = pkg,
        appName = appName,
        title = title,
        text = text,
        subText = null,
        category = null,
        priority = 0,
        timestamp = timestamp,
        groupKey = groupKey,
        isOngoing = false,
        provisionalLane = lane,
    )

    // -- toLane --

    @Test
    fun toLane_mapsEveryProvisionalLane() {
        assertEquals(Lane.Live, toLane(ProvisionalLane.PRIORITY))
        assertEquals(Lane.People, toLane(ProvisionalLane.PEOPLE))
        assertEquals(Lane.Work, toLane(ProvisionalLane.UPDATES))
        assertEquals(Lane.Noise, toLane(ProvisionalLane.QUIET))
    }

    @Test
    fun lane_nullOrUnknownName_fallsBackToUpdatesLane() {
        assertEquals(Lane.Work, toLane(null))
        assertEquals(Lane.Work, row(1, lane = null).lane)
        assertEquals(Lane.Work, row(1, lane = "BOGUS").lane)
        assertEquals(Lane.Noise, row(1, lane = "QUIET").lane)
    }

    // -- sectionize --

    @Test
    fun sectionize_splitsNowEarlierTodayAndOlder() {
        val justNow = row(1, timestamp = now - 5 * 60_000L)
        val edgeOfHour = row(2, timestamp = now - 60 * 60_000L) // exactly 1h ago: not "Now"
        val morning = row(3, timestamp = at(2026, 11, 4, 0, 0)) // start of day boundary
        val lateYesterday = row(4, timestamp = at(2026, 11, 3, 23, 59))
        val lastWeek = row(5, timestamp = at(2026, 10, 28, 9, 0))

        val sections = sectionize(listOf(justNow, edgeOfHour, morning, lateYesterday, lastWeek), now, zone)

        assertEquals(
            listOf(
                NowSection.NOW to listOf(justNow),
                NowSection.EARLIER_TODAY to listOf(edgeOfHour, morning),
                NowSection.OLDER to listOf(lateYesterday, lastWeek),
            ),
            sections,
        )
    }

    @Test
    fun sectionize_emptyInput_returnsNoSections() {
        assertTrue(sectionize(emptyList(), now, zone).isEmpty())
    }

    @Test
    fun sectionize_omitsEmptySections_andTreatsFutureRowsAsNow() {
        val future = row(1, timestamp = now + 10 * 60_000L)
        val old = row(2, timestamp = at(2026, 1, 1, 12, 0))

        val sections = sectionize(listOf(future, old), now, zone)

        assertEquals(listOf(NowSection.NOW, NowSection.OLDER), sections.map { it.first })
    }

    @Test
    fun sectionize_withinFirstHourOfDay_yesterdayRowIsNowNotEarlierToday() {
        val shortlyAfterMidnight = at(2026, 11, 4, 0, 20)
        val tenMinutesBeforeMidnight = row(1, timestamp = at(2026, 11, 3, 23, 50))

        val sections = sectionize(listOf(tenMinutesBeforeMidnight), shortlyAfterMidnight, zone)

        assertEquals(NowSection.NOW, sections.single().first)
    }

    @Test
    fun sectionize_usesGivenZoneForDayBoundary() {
        // 03:00 UTC on Nov 4 is still Nov 3 in New York.
        val utcNow = at(2026, 11, 4, 15, 0)
        val r = row(1, timestamp = java.time.Instant.parse("2026-11-04T03:00:00Z").toEpochMilli())

        assertEquals(NowSection.OLDER, sectionize(listOf(r), utcNow, zone).single().first)
        assertEquals(NowSection.EARLIER_TODAY, sectionize(listOf(r), utcNow, ZoneId.of("UTC")).single().first)
    }

    // -- bundle --

    @Test
    fun bundle_sameGroupKeyWithTwoRows_becomesBundle() {
        val a = row(1, timestamp = now, groupKey = "thread-1")
        val other = row(2, timestamp = now - 1, pkg = "com.example.mail", appName = "Mail")
        val b = row(3, timestamp = now - 2, groupKey = "thread-1")

        val items = bundle(listOf(a, other, b))

        assertEquals(2, items.size)
        val bundleItem = items[0] as BundleItem
        assertEquals(listOf(a, b), bundleItem.rows)
        assertEquals("thread-1", bundleItem.groupKey)
        assertEquals(2, bundleItem.count)
        assertEquals(now, bundleItem.timestamp)
        assertEquals(Lane.People, bundleItem.lane)
        assertSame(other, (items[1] as SingleItem).record)
    }

    @Test
    fun bundle_singleRowGroup_staysSingle() {
        val a = row(1, groupKey = "thread-1")
        val b = row(2, groupKey = "thread-2")

        val items = bundle(listOf(a, b))

        assertEquals(listOf(SingleItem(a), SingleItem(b)), items)
    }

    @Test
    fun bundle_sameGroupKeyDifferentPackages_doNotMerge() {
        val a = row(1, pkg = "com.a", groupKey = "g")
        val b = row(2, pkg = "com.b", groupKey = "g")

        assertTrue(bundle(listOf(a, b)).all { it is SingleItem })
    }

    @Test
    fun bundle_updatesAndQuietLanesBundlePerApp_evenWithoutGroupKey() {
        val u1 = row(1, pkg = "com.shop", appName = "Shop", lane = "UPDATES")
        val q1 = row(2, pkg = "com.shop", appName = "Shop", lane = "QUIET", groupKey = "promo")
        val u2 = row(3, pkg = "com.shop", appName = "Shop", lane = "UPDATES", groupKey = "orders")
        val q2 = row(4, pkg = "com.shop", appName = "Shop", lane = "QUIET")
        val otherApp = row(5, pkg = "com.news", appName = "News", lane = "UPDATES")

        val items = bundle(listOf(u1, q1, u2, q2, otherApp))

        assertEquals(3, items.size)
        val updates = items[0] as BundleItem
        assertEquals(listOf(u1, u2), updates.rows)
        assertEquals(Lane.Work, updates.lane)
        val quiet = items[1] as BundleItem
        assertEquals(listOf(q1, q2), quiet.rows)
        assertEquals(Lane.Noise, quiet.lane)
        assertEquals(SingleItem(otherApp), items[2])
        assertTrue(updates.id != quiet.id)
    }

    @Test
    fun bundle_peopleAndPriorityWithoutGroupKey_neverBundle() {
        val rows = listOf(row(1), row(2), row(3, lane = "PRIORITY"), row(4, lane = "PRIORITY"))

        assertEquals(rows.map { SingleItem(it) }, bundle(rows))
    }

    @Test
    fun bundle_emptyAndBlankGroupKey() {
        assertTrue(bundle(emptyList()).isEmpty())
        val rows = listOf(row(1, groupKey = ""), row(2, groupKey = ""))
        assertTrue(bundle(rows).all { it is SingleItem })
    }

    @Test
    fun streamItemIds_areUniquePerItem() {
        val items = bundle(listOf(row(1), row(2), row(3, groupKey = "g"), row(4, groupKey = "g")))

        assertEquals(items.size, items.map { it.id }.toSet().size)
    }

    // -- filter --

    @Test
    fun filter_matchesTitleTextAndAppNameCaseInsensitively() {
        val byTitle = row(1, title = "Dinner plans", text = "x", appName = "Chat")
        val byText = row(2, title = "x", text = "Your PARCEL shipped", appName = "Mail")
        val byApp = row(3, title = "x", text = "y", appName = "Bank of Things")
        val none = row(4, title = "x", text = "y", appName = "Chat")

        val all = listOf(byTitle, byText, byApp, none)

        assertEquals(listOf(byTitle), filter(all, "dinner"))
        assertEquals(listOf(byText), filter(all, "parcel"))
        assertEquals(listOf(byApp), filter(all, "  BANK "))
    }

    @Test
    fun filter_blankQueryReturnsAll_andNullFieldsDoNotMatchOrCrash() {
        val nulls = row(1, title = null, text = null, appName = "App")
        val rows = listOf(nulls, row(2))

        assertSame(rows, filter(rows, ""))
        assertSame(rows, filter(rows, "   "))
        assertTrue(filter(rows, "zzz").isEmpty())
        assertEquals(listOf(nulls), filter(rows, "app"))
    }

    @Test
    fun filter_treatsRegexAndLikeCharactersLiterally() {
        val pct = row(1, title = "50% off")
        val plain = row(2, title = "500 off")

        assertEquals(listOf(pct), filter(listOf(pct, plain), "%"))
        assertTrue(filter(listOf(plain), ".*").isEmpty())
    }
}
