package com.usagecontrol.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.usagecontrol.core.database.UsageControlDatabase
import org.junit.Assert.assertEquals
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
}
