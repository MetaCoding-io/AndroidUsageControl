package com.usagecontrol.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.LaneDao
import com.usagecontrol.core.database.dao.LiveNotificationDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RouteDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.SoundPresetDao
import com.usagecontrol.core.database.dao.SuggestionDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.entity.ContextSnapshotEntity
import com.usagecontrol.core.database.entity.LaneEntity
import com.usagecontrol.core.database.entity.LiveNotificationEntity
import com.usagecontrol.core.database.entity.NotificationEntity
import com.usagecontrol.core.database.entity.RouteEntity
import com.usagecontrol.core.database.entity.RuleEntity
import com.usagecontrol.core.database.entity.SoundPresetEntity
import com.usagecontrol.core.database.entity.SuggestionEntity
import com.usagecontrol.core.database.entity.UsageEntity
import com.usagecontrol.core.model.DefaultLanes

@Database(
    entities = [
        NotificationEntity::class,
        UsageEntity::class,
        RuleEntity::class,
        SoundPresetEntity::class,
        ContextSnapshotEntity::class,
        SuggestionEntity::class,
        LiveNotificationEntity::class,
        LaneEntity::class,
        RouteEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class UsageControlDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun usageDao(): UsageDao
    abstract fun ruleDao(): RuleDao
    abstract fun soundPresetDao(): SoundPresetDao
    abstract fun contextSnapshotDao(): ContextSnapshotDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun liveNotificationDao(): LiveNotificationDao
    abstract fun laneDao(): LaneDao
    abstract fun routeDao(): RouteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rules ADD COLUMN conditionGroupsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS suggestions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        appPackageName TEXT,
                        appName TEXT,
                        reason TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        ruleTemplateJson TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        resolvedAt INTEGER,
                        acceptedRuleId INTEGER
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Column DDL must match the generated schemas/.../4.json exactly.
                listOf(
                    "`key` TEXT",
                    "`notifId` INTEGER",
                    "`tag` TEXT",
                    "`channelId` TEXT",
                    "`importance` INTEGER NOT NULL DEFAULT 0",
                    "`isConversation` INTEGER NOT NULL DEFAULT 0",
                    "`shortcutId` TEXT",
                    "`whenMs` INTEGER NOT NULL DEFAULT 0",
                    "`isGroupSummary` INTEGER NOT NULL DEFAULT 0",
                    "`flags` INTEGER NOT NULL DEFAULT 0",
                    "`updateCount` INTEGER NOT NULL DEFAULT 0",
                    "`removalReason` TEXT",
                    "`removedAt` INTEGER",
                    "`smallIconPath` TEXT",
                    "`largeIconPath` TEXT",
                    "`color` INTEGER NOT NULL DEFAULT 0",
                    "`provisionalLane` TEXT",
                    "`messagesJson` TEXT",
                    "`actionsJson` TEXT",
                ).forEach { column ->
                    db.execSQL("ALTER TABLE `notifications` ADD COLUMN $column")
                }
                // Legacy rows keep key = NULL; SQLite UNIQUE allows multiple NULLs.
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notifications_key` ON `notifications` (`key`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `live_notifications` (`key` TEXT NOT NULL, " +
                        "`packageName` TEXT NOT NULL, `title` TEXT, `text` TEXT, `category` TEXT, " +
                        "`channelId` TEXT, `isOngoing` INTEGER NOT NULL, `postTime` INTEGER NOT NULL, " +
                        "`smallIconPath` TEXT, `flags` INTEGER NOT NULL, PRIMARY KEY(`key`))"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // DDL must match the generated schemas/.../5.json exactly.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `lanes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `policy` TEXT NOT NULL, " +
                        "`decayMinutes` INTEGER NOT NULL, `sound` TEXT, `breakthroughsJson` TEXT NOT NULL, " +
                        "`isDefault` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `packageName` TEXT NOT NULL, `key` TEXT NOT NULL, " +
                        "`laneId` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_routes_kind_packageName_key` " +
                        "ON `routes` (`kind`, `packageName`, `key`)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_routes_laneId` ON `routes` (`laneId`)")
                db.execSQL("ALTER TABLE `notifications` ADD COLUMN `laneId` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_laneId` ON `notifications` (`laneId`)")
                seedDefaultLanes(db)
            }
        }

        /** Seeds [DefaultLanes] on fresh installs (upgrades are seeded by [MIGRATION_4_5]). */
        val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedDefaultLanes(db)
            }
        }

        /** Idempotent: existing rows with the fixed default ids are kept. */
        fun seedDefaultLanes(db: SupportSQLiteDatabase) {
            DefaultLanes.all.forEach { lane ->
                db.execSQL(
                    "INSERT OR IGNORE INTO `lanes` (`id`, `name`, `colorArgb`, `policy`, `decayMinutes`, " +
                        "`sound`, `breakthroughsJson`, `isDefault`, `sortOrder`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(
                        lane.id, lane.name, lane.colorArgb, lane.policy.name, lane.decayMinutes,
                        lane.sound, lane.breakthroughsJson, if (lane.isDefault) 1 else 0, lane.sortOrder,
                    ),
                )
            }
        }
    }
}
