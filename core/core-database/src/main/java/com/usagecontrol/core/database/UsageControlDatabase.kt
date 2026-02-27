package com.usagecontrol.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.SoundPresetDao
import com.usagecontrol.core.database.dao.SuggestionDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.entity.ContextSnapshotEntity
import com.usagecontrol.core.database.entity.NotificationEntity
import com.usagecontrol.core.database.entity.RuleEntity
import com.usagecontrol.core.database.entity.SoundPresetEntity
import com.usagecontrol.core.database.entity.SuggestionEntity
import com.usagecontrol.core.database.entity.UsageEntity

@Database(
    entities = [
        NotificationEntity::class,
        UsageEntity::class,
        RuleEntity::class,
        SoundPresetEntity::class,
        ContextSnapshotEntity::class,
        SuggestionEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class UsageControlDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun usageDao(): UsageDao
    abstract fun ruleDao(): RuleDao
    abstract fun soundPresetDao(): SoundPresetDao
    abstract fun contextSnapshotDao(): ContextSnapshotDao
    abstract fun suggestionDao(): SuggestionDao

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
    }
}
