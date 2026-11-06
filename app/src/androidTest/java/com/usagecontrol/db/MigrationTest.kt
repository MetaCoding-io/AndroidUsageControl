package com.usagecontrol.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.usagecontrol.core.database.UsageControlDatabase
import com.usagecontrol.core.model.DefaultLanes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        UsageControlDatabase::class.java,
    )

    @Test
    fun migrate3To4_preservesRows() {
        helper.createDatabase(dbName, 3).use { db ->
            val rows = listOf(
                Triple("com.example.a", 1_000L, "NONE"),
                Triple("com.example.b", 2_000L, "DISMISSED"),
                Triple("com.example.c", 3_000L, "OPENED"),
            )
            rows.forEach { (pkg, ts, action) ->
                db.execSQL(
                    "INSERT INTO notifications (packageName, appName, title, text, subText, category, " +
                        "priority, timestamp, groupKey, isOngoing, userAction, actionTimestamp, " +
                        "ruleIdIfAutoDismissed) VALUES (?, ?, 'title', 'text', NULL, NULL, 0, ?, NULL, 0, ?, NULL, NULL)",
                    arrayOf<Any>(pkg, pkg, ts, action),
                )
            }
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, UsageControlDatabase.MIGRATION_3_4)

        db.query(
            "SELECT packageName, timestamp, userAction, `key`, updateCount, removalReason " +
                "FROM notifications ORDER BY timestamp"
        ).use { c ->
            assertEquals(3, c.count)
            val expected = listOf(
                Triple("com.example.a", 1_000L, "NONE"),
                Triple("com.example.b", 2_000L, "DISMISSED"),
                Triple("com.example.c", 3_000L, "OPENED"),
            )
            expected.forEach { (pkg, ts, action) ->
                assertTrue(c.moveToNext())
                assertEquals(pkg, c.getString(0))
                assertEquals(ts, c.getLong(1))
                assertEquals(action, c.getString(2))
                assertTrue("legacy key must stay NULL", c.isNull(3))
                assertEquals(0, c.getInt(4))
                assertTrue(c.isNull(5))
            }
        }

        db.query("SELECT COUNT(*) FROM live_notifications").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate4To5_addsLanesRoutesAndNullLaneId() {
        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL(
                "INSERT INTO notifications (packageName, appName, title, text, subText, category, " +
                    "priority, timestamp, groupKey, isOngoing, userAction, actionTimestamp, " +
                    "ruleIdIfAutoDismissed, `key`, channelId) VALUES ('com.example.a', 'A', 't', 'x', NULL, NULL, " +
                    "0, 1000, NULL, 0, 'NONE', NULL, NULL, 'k1', 'promo')"
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, UsageControlDatabase.MIGRATION_4_5)

        db.query("SELECT `key`, channelId, laneId FROM notifications").use { c ->
            assertEquals(1, c.count)
            assertTrue(c.moveToFirst())
            assertEquals("k1", c.getString(0))
            assertEquals("promo", c.getString(1))
            assertTrue("existing rows are unrouted", c.isNull(2))
        }

        db.query("SELECT id, name, policy, isDefault FROM lanes ORDER BY sortOrder").use { c ->
            assertEquals(6, c.count)
            DefaultLanes.all.forEach { lane ->
                assertTrue(c.moveToNext())
                assertEquals(lane.id, c.getLong(0))
                assertEquals(lane.name, c.getString(1))
                assertEquals(lane.policy.name, c.getString(2))
                assertEquals(1, c.getInt(3))
            }
            assertFalse(c.moveToNext())
        }

        db.query("SELECT COUNT(*) FROM routes").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }

        // Re-seeding is idempotent (INSERT OR IGNORE on fixed ids).
        UsageControlDatabase.seedDefaultLanes(db)
        db.query("SELECT COUNT(*) FROM lanes").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(6, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate3To5_chainsMigrations() {
        helper.createDatabase(dbName, 3).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 5, true, UsageControlDatabase.MIGRATION_3_4, UsageControlDatabase.MIGRATION_4_5,
        )
        db.query("SELECT COUNT(*) FROM lanes").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(6, c.getInt(0))
        }
        db.close()
    }
}
