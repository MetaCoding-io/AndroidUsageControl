package com.usagecontrol.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.usagecontrol.core.database.UsageControlDatabase
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.entity.NotificationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {

    private lateinit var db: UsageControlDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UsageControlDatabase::class.java,
        ).build()
        dao = db.notificationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        key: String?,
        text: String = "hello",
        timestamp: Long = 10_000L,
        isOngoing: Boolean = false,
        pkg: String = "com.example.chat",
        isGroupSummary: Boolean = false,
    ) = NotificationEntity(
        packageName = pkg,
        appName = "Chat",
        title = "Alice",
        text = text,
        subText = null,
        category = null,
        priority = 0,
        timestamp = timestamp,
        groupKey = null,
        isOngoing = isOngoing,
        userAction = "NONE",
        actionTimestamp = null,
        ruleIdIfAutoDismissed = null,
        key = key,
        isGroupSummary = isGroupSummary,
    )

    @Test
    fun upsertTwiceSameKey_singleRowWithUpdateCount() = runBlocking {
        val first = dao.upsertByKey(entity("0|com.example.chat|1|null|10001"))
        val second = dao.upsertByKey(entity("0|com.example.chat|1|null|10001", text = "hello again", timestamp = 20_000L))

        assertEquals(first, second)
        assertEquals(1, dao.getCount())
        val row = dao.getById(first)!!
        assertEquals(1, row.updateCount)
        assertEquals("hello again", row.text)
        assertEquals(20_000L, row.timestamp)
    }

    @Test
    fun differentKeysAndNullKeys_insertSeparateRows() = runBlocking {
        val a = dao.upsertByKey(entity("k1"))
        val b = dao.upsertByKey(entity("k2"))
        val c = dao.upsertByKey(entity(null))
        val d = dao.upsertByKey(entity(null))

        assertEquals(4, setOf(a, b, c, d).size)
        assertEquals(4, dao.getCount())
    }

    @Test
    fun markRemoved_setsReasonAndKeepsCount() = runBlocking {
        val id = dao.upsertByKey(entity("k1"))
        dao.upsertByKey(entity("k1", text = "edit"))

        dao.markRemoved(id, "CANCEL", 30_000L)

        val row = dao.getById(id)!!
        assertEquals("CANCEL", row.removalReason)
        assertEquals(30_000L, row.removedAt)
        assertEquals("DISMISSED", row.userAction)
        assertEquals(1, row.updateCount)
        assertNull(dao.findIdByKey("k1"))
        assertEquals(1, dao.getCount())
    }

    @Test
    fun markRemoved_clickMapsToOpened_andPreservesExistingAction() = runBlocking {
        val clicked = dao.upsertByKey(entity("click"))
        dao.markRemoved(clicked, "CLICK", 1L)
        assertEquals("OPENED", dao.getById(clicked)!!.userAction)

        val auto = dao.upsertByKey(entity("auto"))
        dao.markAutoDismissed(auto, ruleId = 7L, timestamp = 5L)
        dao.markRemoved(auto, "LISTENER_CANCEL", 6L)
        val autoRow = dao.getById(auto)!!
        assertEquals("AUTO_DISMISSED", autoRow.userAction)
        assertEquals(5L, autoRow.actionTimestamp)
        assertEquals("LISTENER_CANCEL", autoRow.removalReason)
    }

    @Test
    fun repostAfterRemoval_startsNewRow() = runBlocking {
        val first = dao.upsertByKey(entity("k1"))
        dao.markRemoved(first, "CANCEL", 1L)

        val second = dao.upsertByKey(entity("k1", text = "new"))

        assertNotEquals(first, second)
        assertEquals(2, dao.getCount())
        assertEquals(0, dao.getById(second)!!.updateCount)
        assertNull(dao.getById(first)!!.key)
        assertEquals("CANCEL", dao.getById(first)!!.removalReason)
    }

    @Test
    fun ongoingRowsExcludedFromStats() = runBlocking {
        dao.upsertByKey(entity("k1", pkg = "com.example.chat"))
        dao.upsertByKey(entity("k2", pkg = "com.example.music", isOngoing = true))

        val stats = dao.getDismissStats(0L)

        assertEquals(listOf("com.example.chat"), stats.map { it.packageName })
        assertEquals(1, dao.getActionBreakdown(0L).total)
        assertEquals(2, dao.getCount())
    }

    @Test
    fun nowStream_includesLiveAndRecent_excludesSummaryOngoingArchivedAndOldLegacy() = runBlocking {
        val live = dao.upsertByKey(entity("live", timestamp = 1_000L))
        val recentRemoved = dao.upsertByKey(entity("recent", timestamp = 50_000L))
        dao.markRemoved(recentRemoved, "CANCEL", 51_000L)
        val oldRemoved = dao.upsertByKey(entity("old", timestamp = 2_000L))
        dao.markRemoved(oldRemoved, "CANCEL", 3_000L)
        dao.upsertByKey(entity(null, timestamp = 3_000L)) // legacy row: no key, never removed
        val recentLegacy = dao.upsertByKey(entity(null, timestamp = 60_000L))
        dao.upsertByKey(entity("summary", timestamp = 70_000L, isGroupSummary = true))
        dao.upsertByKey(entity("ongoing", timestamp = 70_000L, isOngoing = true))
        val archived = dao.upsertByKey(entity("archived", timestamp = 70_000L))
        dao.markUserAction(archived, "ARCHIVED", 71_000L)

        val ids = dao.nowStreamFlow(since = 40_000L).first().map { it.id }

        assertEquals(listOf(recentLegacy, recentRemoved, live), ids)
    }

    @Test
    fun nowStream_respectsLimit() = runBlocking {
        repeat(5) { dao.upsertByKey(entity("k$it", timestamp = 1_000L + it)) }

        assertEquals(3, dao.nowStreamFlow(since = 0L, limit = 3).first().size)
    }

    @Test
    fun clearUserAction_restoresArchivedRowToNowStream() = runBlocking {
        val id = dao.upsertByKey(entity("k1"))
        assertEquals(1, dao.markUserAction(id, "ARCHIVED", 5L))
        assertTrue(dao.nowStreamFlow(0L).first().isEmpty())

        assertEquals(1, dao.clearUserAction(id))

        val row = dao.getById(id)!!
        assertEquals("NONE", row.userAction)
        assertNull(row.actionTimestamp)
        assertEquals(listOf(id), dao.nowStreamFlow(0L).first().map { it.id })
    }

    @Test
    fun markAndClearUserAction_unknownId_updatesNothing() = runBlocking {
        assertEquals(0, dao.markUserAction(999L, "ARCHIVED", 1L))
        assertEquals(0, dao.clearUserAction(999L))
    }

    @Test
    fun archivedThenRemoved_keepsArchivedAction() = runBlocking {
        val id = dao.upsertByKey(entity("k1"))
        dao.markUserAction(id, "ARCHIVED", 5L)
        dao.markRemoved(id, "LISTENER_CANCEL", 6L)

        val row = dao.getById(id)!!
        assertEquals("ARCHIVED", row.userAction)
        assertEquals(5L, row.actionTimestamp)
    }

    @Test
    fun recentlyDismissed_orderedByRemovedAt_excludesLiveAndOld() = runBlocking {
        dao.upsertByKey(entity("live"))
        val old = dao.upsertByKey(entity("old"))
        dao.markRemoved(old, "CANCEL", 1_000L)
        val a = dao.upsertByKey(entity("a", timestamp = 100L))
        dao.markRemoved(a, "CANCEL", 20_000L)
        val b = dao.upsertByKey(entity("b", timestamp = 200L))
        dao.markRemoved(b, "CLICK", 30_000L)
        val summary = dao.upsertByKey(entity("s", isGroupSummary = true))
        dao.markRemoved(summary, "CANCEL", 30_000L)

        val ids = dao.recentlyDismissedFlow(since = 10_000L).first().map { it.id }

        assertEquals(listOf(b, a), ids)
    }

    @Test
    fun todayInterruptionCount_countsNonOngoingNonSummarySinceStartOfDay() = runBlocking {
        dao.upsertByKey(entity("yesterday", timestamp = 500L))
        dao.upsertByKey(entity("t1", timestamp = 2_000L))
        dao.upsertByKey(entity("t2", timestamp = 3_000L))
        dao.upsertByKey(entity("ongoing", timestamp = 3_000L, isOngoing = true))
        dao.upsertByKey(entity("summary", timestamp = 3_000L, isGroupSummary = true))

        assertEquals(2, dao.todayInterruptionCount(startOfDay = 1_000L).first())
        assertEquals(0, dao.todayInterruptionCount(startOfDay = 10_000L).first())
    }

    @Test
    fun appAlertCounts_countsUserDismissalsOnly_excludesOngoingSummaryAndOld() = runBlocking {
        val swiped = dao.upsertByKey(entity("swiped", timestamp = 2_000L))
        dao.markRemoved(swiped, "CANCEL", 3_000L)
        val appCancel = dao.upsertByKey(entity("appCancel", timestamp = 2_000L))
        dao.markRemoved(appCancel, "APP_CANCEL", 3_000L)
        val archived = dao.upsertByKey(entity("archived", timestamp = 2_000L))
        dao.markUserAction(archived, "ARCHIVED", 3_000L)
        dao.insert(entity(null, timestamp = 2_000L).copy(userAction = "DISMISSED")) // legacy v3
        dao.upsertByKey(entity("live", timestamp = 2_000L))
        dao.upsertByKey(entity("old", timestamp = 500L))
        dao.upsertByKey(entity("ongoing", timestamp = 2_000L, isOngoing = true))
        dao.upsertByKey(entity("summary", timestamp = 2_000L, isGroupSummary = true))
        dao.upsertByKey(entity("other", timestamp = 2_000L, pkg = "com.example.mail"))

        val rows = dao.appAlertCountsFlow(since = 1_000L).first()

        assertEquals(listOf("com.example.chat", "com.example.mail"), rows.map { it.packageName })
        assertEquals(5, rows[0].total)
        assertEquals(3, rows[0].dismissed)
        assertEquals(0, rows[1].dismissed)
    }

    @Test
    fun appDailyCounts_groupsByPackageAndDay() = runBlocking {
        dao.upsertByKey(entity("a", timestamp = 2_000L))
        dao.upsertByKey(entity("b", timestamp = 3_000L))
        dao.upsertByKey(entity("c", timestamp = 3_000L, pkg = "com.example.mail"))

        val rows = dao.appDailyCountsFlow(since = 1_000L).first()

        assertEquals(2, rows.size)
        assertEquals(2, rows.single { it.packageName == "com.example.chat" }.count)
    }

    @Test
    fun appEvents_returnsTimesForOnePackageOldestFirst() = runBlocking {
        val late = dao.upsertByKey(entity("late", timestamp = 5_000L).copy(channelId = "msg"))
        dao.markRemoved(late, "CLICK", 6_000L)
        dao.upsertByKey(entity("early", timestamp = 2_000L))
        dao.upsertByKey(entity("old", timestamp = 500L))
        dao.upsertByKey(entity("other", timestamp = 3_000L, pkg = "com.example.mail"))

        val rows = dao.appEventsFlow("com.example.chat", since = 1_000L).first()

        assertEquals(listOf(2_000L, 5_000L), rows.map { it.timestamp })
        assertNull(rows[0].actionTimestamp)
        assertEquals(6_000L, rows[1].actionTimestamp)
        assertEquals(6_000L, rows[1].removedAt)
        assertEquals("msg", rows[1].channelId)
    }

    @Test
    fun appChannelCounts_groupsIncludingNullChannel() = runBlocking {
        dao.upsertByKey(entity("a", timestamp = 2_000L).copy(channelId = "msg"))
        dao.upsertByKey(entity("b", timestamp = 2_000L).copy(channelId = "msg"))
        dao.upsertByKey(entity("c", timestamp = 2_000L))

        val rows = dao.appChannelCountsFlow("com.example.chat", since = 1_000L).first()

        assertEquals(2, rows.size)
        assertEquals("msg", rows[0].channelId)
        assertEquals(2, rows[0].count)
        assertNull(rows[1].channelId)
    }
}
