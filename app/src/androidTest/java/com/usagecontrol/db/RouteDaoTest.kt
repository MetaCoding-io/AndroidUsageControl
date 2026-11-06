package com.usagecontrol.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.usagecontrol.core.database.LaneRepository
import com.usagecontrol.core.database.UsageControlDatabase
import com.usagecontrol.core.database.dao.RouteDao
import com.usagecontrol.core.database.entity.NotificationEntity
import com.usagecontrol.core.database.entity.RouteEntity
import com.usagecontrol.core.model.DefaultLanes
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.LanePolicy
import com.usagecontrol.core.model.RouteKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteDaoTest {

    private lateinit var db: UsageControlDatabase
    private lateinit var routeDao: RouteDao
    private lateinit var repo: LaneRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UsageControlDatabase::class.java,
        ).addCallback(UsageControlDatabase.SEED_CALLBACK).build()
        routeDao = db.routeDao()
        repo = LaneRepository(db, db.laneDao(), routeDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun insert(
        pkg: String,
        key: String,
        channelId: String? = null,
        shortcutId: String? = null,
        title: String? = "Alice",
    ) = runBlocking {
        db.notificationDao().insert(
            NotificationEntity(
                packageName = pkg, appName = pkg, title = title, text = "x", subText = null,
                category = null, priority = 0, timestamp = 1_000L, groupKey = null, isOngoing = false,
                userAction = "NONE", actionTimestamp = null, ruleIdIfAutoDismissed = null,
                key = key, channelId = channelId, shortcutId = shortcutId,
            )
        )
    }

    private fun laneOf(key: String): Long? = db.query("SELECT laneId FROM notifications WHERE `key` = ?", arrayOf(key)).use { c ->
        assertTrue(c.moveToFirst())
        if (c.isNull(0)) null else c.getLong(0)
    }

    @Test
    fun freshDatabase_seedsSixDefaultLanes() = runBlocking {
        val lanes = repo.getLanes()
        assertEquals(DefaultLanes.all.map { it.name }, lanes.map { it.name })
        assertTrue(lanes.all { it.isDefault })
    }

    @Test
    fun appRoute_reFilesOnlyThatApp() = runBlocking {
        insert("com.shop", "a1")
        insert("com.shop", "a2")
        insert("com.other", "b1")

        val count = repo.setRoute(RouteKind.APP, "com.shop", null, DefaultLanes.QUIET_ID)

        assertEquals(2, count)
        assertEquals(DefaultLanes.QUIET_ID, laneOf("a1"))
        assertEquals(DefaultLanes.QUIET_ID, laneOf("a2"))
        assertEquals(null, laneOf("b1"))
    }

    @Test
    fun appRoute_doesNotOverrideMoreSpecificRoutes() = runBlocking {
        insert("com.chat", "p1", shortcutId = "conv-alice")
        insert("com.chat", "c1", channelId = "promo", title = "Deals")
        insert("com.chat", "o1", title = "Bob")

        assertEquals(1, repo.setRoute(RouteKind.PERSON, "com.chat", "conv-alice", DefaultLanes.PEOPLE_ID))
        assertEquals(1, repo.setRoute(RouteKind.CHANNEL, "com.chat", "promo", DefaultLanes.BLOCKED_ID))
        assertEquals(1, repo.setRoute(RouteKind.APP, "com.chat", null, DefaultLanes.BATCHED_ID))

        assertEquals(DefaultLanes.PEOPLE_ID, laneOf("p1"))
        assertEquals(DefaultLanes.BLOCKED_ID, laneOf("c1"))
        assertEquals(DefaultLanes.BATCHED_ID, laneOf("o1"))
    }

    @Test
    fun personRoute_fallsBackToTitleWhenNoShortcut() = runBlocking {
        insert("com.sms", "s1", title = "Mum")
        insert("com.sms", "s2", title = "Spam Co")

        assertEquals(1, routeDao.reFileHistory("com.sms", "Mum", DefaultLanes.PEOPLE_ID, RouteKind.PERSON.name))
        assertEquals(DefaultLanes.PEOPLE_ID, laneOf("s1"))
        assertEquals(null, laneOf("s2"))
    }

    @Test
    fun reFile_unknownSender_updatesNothing() = runBlocking {
        insert("com.shop", "a1")
        assertEquals(0, routeDao.reFileHistory("com.nobody", null, DefaultLanes.QUIET_ID))
        assertEquals(0, routeDao.reFileHistory("com.shop", "no-such-channel", DefaultLanes.QUIET_ID, "CHANNEL"))
        assertEquals(null, laneOf("a1"))
    }

    @Test
    fun setRoute_replacesExistingRouteForSameSender() = runBlocking {
        repo.setRoute(RouteKind.APP, "com.shop", null, DefaultLanes.QUIET_ID)
        repo.setRoute(RouteKind.APP, "com.shop", "", DefaultLanes.DIGEST_ID)
        val routes = repo.getRoutesFor("com.shop")
        assertEquals(1, routes.size)
        assertEquals(DefaultLanes.DIGEST_ID, routes.single().laneId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setRoute_channelWithoutKey_isRejected(): Unit = runBlocking {
        repo.setRoute(RouteKind.CHANNEL, "com.shop", null, DefaultLanes.QUIET_ID)
    }

    @Test
    fun deleteLane_keepsDefaultsAndDropsCustomRoutes() = runBlocking {
        assertFalse(repo.deleteLane(DefaultLanes.INSTANT_ID))
        val custom = repo.saveLane(Lane(name = "Shopping", colorArgb = 0, policy = LanePolicy.Quiet, decayMinutes = 60))
        routeDao.upsert(RouteEntity(kind = "APP", packageName = "com.shop", key = "", laneId = custom, createdAt = 1))
        assertTrue(repo.deleteLane(custom))
        assertTrue(repo.getRoutesFor("com.shop").isEmpty())
        assertEquals(6, repo.getLanes().size)
    }

    @Test
    fun upsertByKey_preservesLaneIdWhenUpdateHasNone() = runBlocking {
        insert("com.shop", "a1")
        repo.setRoute(RouteKind.APP, "com.shop", null, DefaultLanes.QUIET_ID)
        db.notificationDao().upsertByKey(
            NotificationEntity(
                packageName = "com.shop", appName = "com.shop", title = "t2", text = "y", subText = null,
                category = null, priority = 0, timestamp = 2_000L, groupKey = null, isOngoing = false,
                userAction = "NONE", actionTimestamp = null, ruleIdIfAutoDismissed = null, key = "a1",
            )
        )
        assertEquals(DefaultLanes.QUIET_ID, laneOf("a1"))
    }
}
