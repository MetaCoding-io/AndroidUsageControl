package com.usagecontrol.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.SoundPresetDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.entity.ContextSnapshotEntity
import com.usagecontrol.core.database.entity.NotificationEntity
import com.usagecontrol.core.database.entity.RuleEntity
import com.usagecontrol.core.database.entity.SoundPresetEntity
import com.usagecontrol.core.database.entity.UsageEntity

@Database(
    entities = [
        NotificationEntity::class,
        UsageEntity::class,
        RuleEntity::class,
        SoundPresetEntity::class,
        ContextSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class UsageControlDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun usageDao(): UsageDao
    abstract fun ruleDao(): RuleDao
    abstract fun soundPresetDao(): SoundPresetDao
    abstract fun contextSnapshotDao(): ContextSnapshotDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rules ADD COLUMN conditionGroupsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
