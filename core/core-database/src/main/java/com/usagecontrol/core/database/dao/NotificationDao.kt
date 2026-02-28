package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.usagecontrol.core.database.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(notification: NotificationEntity): Long

    @Update
    suspend fun update(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getByPackageFlow(packageName: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getById(id: Long): NotificationEntity?

    @Query("SELECT DISTINCT packageName, appName FROM notifications ORDER BY appName")
    fun getDistinctAppsFlow(): Flow<List<AppInfo>>

    @Query("""
        SELECT * FROM notifications
        WHERE (:packageName IS NULL OR packageName = :packageName)
        AND (:searchQuery IS NULL OR title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%')
        AND timestamp >= :startTime
        AND timestamp <= :endTime
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun search(
        packageName: String?,
        searchQuery: String?,
        startTime: Long,
        endTime: Long,
        limit: Int = 50,
        offset: Int = 0,
    ): List<NotificationEntity>

    @Query("UPDATE notifications SET userAction = :action, actionTimestamp = :timestamp WHERE id = :id")
    suspend fun updateAction(id: Long, action: String, timestamp: Long)

    @Query("UPDATE notifications SET userAction = 'AUTO_DISMISSED', ruleIdIfAutoDismissed = :ruleId, actionTimestamp = :timestamp WHERE id = :id")
    suspend fun markAutoDismissed(id: Long, ruleId: Long, timestamp: Long)

    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getCount(): Int

    // Hour-of-day distribution of dismissed notifications per app
    @Query("""
        SELECT packageName, appName,
            CAST(strftime('%H', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) as hour,
            COUNT(*) as count
        FROM notifications
        WHERE userAction = 'DISMISSED' AND timestamp >= :sinceTimestamp
        GROUP BY packageName, hour
        ORDER BY packageName, hour
    """)
    suspend fun getDismissalsByHour(sinceTimestamp: Long): List<HourlyDismissRow>

    // Stats queries for learning
    @Query("""
        SELECT packageName, appName,
            COUNT(*) as totalNotifications,
            SUM(CASE WHEN userAction = 'DISMISSED' THEN 1 ELSE 0 END) as dismissedCount,
            SUM(CASE WHEN userAction = 'OPENED' THEN 1 ELSE 0 END) as openedCount,
            SUM(CASE WHEN userAction = 'AUTO_DISMISSED' THEN 1 ELSE 0 END) as autoDismissedCount,
            AVG(CASE WHEN actionTimestamp IS NOT NULL THEN actionTimestamp - timestamp ELSE NULL END) as avgTimeToActionMs
        FROM notifications
        WHERE timestamp >= :sinceTimestamp
        GROUP BY packageName
        ORDER BY totalNotifications DESC
    """)
    suspend fun getDismissStats(sinceTimestamp: Long): List<DismissStatsRow>

    // Daily notification counts for trend chart
    @Query("""
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') as date,
            COUNT(*) as total,
            SUM(CASE WHEN userAction = 'DISMISSED' THEN 1 ELSE 0 END) as dismissed,
            SUM(CASE WHEN userAction = 'OPENED' THEN 1 ELSE 0 END) as opened,
            SUM(CASE WHEN userAction = 'AUTO_DISMISSED' THEN 1 ELSE 0 END) as autoDismissed
        FROM notifications
        WHERE timestamp >= :sinceTimestamp
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyStats(sinceTimestamp: Long): List<DailyNotifStatsRow>

    // Action breakdown for all notifications in period
    @Query("""
        SELECT
            SUM(CASE WHEN userAction = 'DISMISSED' THEN 1 ELSE 0 END) as dismissed,
            SUM(CASE WHEN userAction = 'OPENED' THEN 1 ELSE 0 END) as opened,
            SUM(CASE WHEN userAction = 'AUTO_DISMISSED' THEN 1 ELSE 0 END) as autoDismissed,
            SUM(CASE WHEN userAction = 'NONE' THEN 1 ELSE 0 END) as pending,
            COUNT(*) as total
        FROM notifications
        WHERE timestamp >= :sinceTimestamp
    """)
    suspend fun getActionBreakdown(sinceTimestamp: Long): ActionBreakdownRow
}

data class AppInfo(
    val packageName: String,
    val appName: String,
)

data class HourlyDismissRow(
    val packageName: String,
    val appName: String,
    val hour: Int,
    val count: Int,
)

data class DismissStatsRow(
    val packageName: String,
    val appName: String,
    val totalNotifications: Int,
    val dismissedCount: Int,
    val openedCount: Int,
    val autoDismissedCount: Int,
    val avgTimeToActionMs: Long?,
)

data class DailyNotifStatsRow(
    val date: String,
    val total: Int,
    val dismissed: Int,
    val opened: Int,
    val autoDismissed: Int,
)

data class ActionBreakdownRow(
    val dismissed: Int,
    val opened: Int,
    val autoDismissed: Int,
    val pending: Int,
    val total: Int,
)
