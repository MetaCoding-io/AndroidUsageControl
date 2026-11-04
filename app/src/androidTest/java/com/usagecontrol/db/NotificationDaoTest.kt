package com.usagecontrol.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.usagecontrol.core.database.UsageControlDatabase
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.entity.NotificationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
}
