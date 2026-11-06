package com.usagecontrol.core.database

import android.content.Context
import androidx.room.Room
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.LaneDao
import com.usagecontrol.core.database.dao.LiveNotificationDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RouteDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.SoundPresetDao
import com.usagecontrol.core.database.dao.SuggestionDao
import com.usagecontrol.core.database.dao.UsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UsageControlDatabase {
        return Room.databaseBuilder(
            context,
            UsageControlDatabase::class.java,
            "usage_control.db"
        )
            .addMigrations(
                UsageControlDatabase.MIGRATION_1_2,
                UsageControlDatabase.MIGRATION_2_3,
                UsageControlDatabase.MIGRATION_3_4,
                UsageControlDatabase.MIGRATION_4_5,
            )
            .addCallback(UsageControlDatabase.SEED_CALLBACK)
            .build()
    }

    @Provides
    fun provideNotificationDao(db: UsageControlDatabase): NotificationDao = db.notificationDao()

    @Provides
    fun provideUsageDao(db: UsageControlDatabase): UsageDao = db.usageDao()

    @Provides
    fun provideRuleDao(db: UsageControlDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideSoundPresetDao(db: UsageControlDatabase): SoundPresetDao = db.soundPresetDao()

    @Provides
    fun provideContextSnapshotDao(db: UsageControlDatabase): ContextSnapshotDao = db.contextSnapshotDao()

    @Provides
    fun provideSuggestionDao(db: UsageControlDatabase): SuggestionDao = db.suggestionDao()

    @Provides
    fun provideLiveNotificationDao(db: UsageControlDatabase): LiveNotificationDao = db.liveNotificationDao()

    @Provides
    fun provideLaneDao(db: UsageControlDatabase): LaneDao = db.laneDao()

    @Provides
    fun provideRouteDao(db: UsageControlDatabase): RouteDao = db.routeDao()
}
