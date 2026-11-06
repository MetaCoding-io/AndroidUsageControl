package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    /** Every icon path still referenced by history or live rows; used to prune the on-disk icon store. */
    @Query("""
        SELECT smallIconPath FROM notifications WHERE smallIconPath IS NOT NULL
        UNION SELECT largeIconPath FROM notifications WHERE largeIconPath IS NOT NULL
        UNION SELECT smallIconPath FROM live_notifications WHERE smallIconPath IS NOT NULL
    """)
    suspend fun getAllIconPaths(): List<String>

    // -- v4 key identity --

    /** Id of the live (not yet removed) history row for this StatusBarNotification key. */
    @Query("SELECT id FROM notifications WHERE `key` = :key AND removedAt IS NULL LIMIT 1")
    suspend fun findIdByKey(key: String): Long?

    /** Detach a removed row from its key so a re-post with the same key starts a new row. */
    @Query("UPDATE notifications SET `key` = NULL WHERE `key` = :key AND removedAt IS NOT NULL")
    suspend fun releaseRemovedKey(key: String): Int

    @Query("""
        UPDATE notifications SET
            title = :title, text = :text, subText = :subText, timestamp = :timestamp,
            whenMs = :whenMs, flags = :flags, importance = :importance,
            smallIconPath = COALESCE(:smallIconPath, smallIconPath),
            largeIconPath = COALESCE(:largeIconPath, largeIconPath),
            messagesJson = :messagesJson, actionsJson = :actionsJson,
            provisionalLane = COALESCE(:provisionalLane, provisionalLane),
            laneId = COALESCE(:laneId, laneId),
            updateCount = updateCount + 1
        WHERE id = :id
    """)
    suspend fun applyUpdate(
        id: Long,
        title: String?,
        text: String?,
        subText: String?,
        timestamp: Long,
        whenMs: Long,
        flags: Int,
        importance: Int,
        smallIconPath: String?,
        largeIconPath: String?,
        messagesJson: String?,
        actionsJson: String?,
        provisionalLane: String?,
        laneId: Long? = null,
    ): Int

    /**
     * Insert a new row for an unseen key, or update the existing live row for that key
     * (content, time, icons) and increment updateCount. Returns the row id. Rows without
     * a key are always inserted.
     */
    @Transaction
    suspend fun upsertByKey(entity: NotificationEntity): Long {
        val key = entity.key ?: return insert(entity)
        val existingId = findIdByKey(key)
        if (existingId == null) {
            releaseRemovedKey(key)
            return insert(entity.copy(id = 0, updateCount = 0))
        }
        applyUpdate(
            id = existingId,
            title = entity.title,
            text = entity.text,
            subText = entity.subText,
            timestamp = entity.timestamp,
            whenMs = entity.whenMs,
            flags = entity.flags,
            importance = entity.importance,
            smallIconPath = entity.smallIconPath,
            largeIconPath = entity.largeIconPath,
            messagesJson = entity.messagesJson,
            actionsJson = entity.actionsJson,
            provisionalLane = entity.provisionalLane,
            laneId = entity.laneId,
        )
        return existingId
    }

    /**
     * Record removal reason/time. userAction becomes OPENED for CLICK, DISMISSED otherwise,
     * but only if no action was recorded yet (e.g. AUTO_DISMISSED is preserved).
     */
    @Query("""
        UPDATE notifications SET
            removalReason = :reason,
            removedAt = :timestamp,
            userAction = CASE WHEN userAction = 'NONE'
                THEN (CASE WHEN :reason = 'CLICK' THEN 'OPENED' ELSE 'DISMISSED' END)
                ELSE userAction END,
            actionTimestamp = CASE WHEN userAction = 'NONE' THEN :timestamp ELSE actionTimestamp END
        WHERE id = :id
    """)
    suspend fun markRemoved(id: Long, reason: String, timestamp: Long): Int

    // -- Now stream --

    /**
     * Rows for the Now stream: live keyed rows (still in the shade) plus anything posted since
     * [since], excluding group summaries, ongoing rows and user-archived or snoozed rows (a
     * snoozed notification reposts as a new row). Legacy v3 rows
     * have no key and never get removedAt, so they only show up through the [since] window.
     */
    @Query("""
        SELECT * FROM notifications
        WHERE ((removedAt IS NULL AND `key` IS NOT NULL) OR timestamp >= :since)
        AND isGroupSummary = 0 AND isOngoing = 0
        AND userAction NOT IN ('ARCHIVED', 'SNOOZED')
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun nowStreamFlow(since: Long, limit: Int = 500): Flow<List<NotificationEntity>>

    /** Rows removed from the shade since [since], most recently removed first. */
    @Query("""
        SELECT * FROM notifications
        WHERE removedAt >= :since AND isGroupSummary = 0 AND isOngoing = 0
        ORDER BY removedAt DESC
        LIMIT :limit
    """)
    fun recentlyDismissedFlow(since: Long, limit: Int = 100): Flow<List<NotificationEntity>>

    /** Interruptions today: non-ongoing, non-summary rows posted or updated since [startOfDay]. */
    @Query("""
        SELECT COUNT(*) FROM notifications
        WHERE timestamp >= :startOfDay AND isOngoing = 0 AND isGroupSummary = 0
    """)
    fun todayInterruptionCount(startOfDay: Long): Flow<Int>

    /** Record a user triage action (e.g. ARCHIVED, SNOOZED) on a row. */
    @Query("UPDATE notifications SET userAction = :action, actionTimestamp = :timestamp WHERE id = :id")
    suspend fun markUserAction(id: Long, action: String, timestamp: Long): Int

    /** Undo a user triage action: back to NONE with no action time. */
    @Query("UPDATE notifications SET userAction = 'NONE', actionTimestamp = NULL WHERE id = :id")
    suspend fun clearUserAction(id: Long): Int

    // -- Apps tab / app profile --
    // "Dismissed" means the user swiped it away (removalReason CANCEL/CANCEL_ALL), archived it, or,
    // for legacy v3 rows without a removalReason, userAction DISMISSED. App-side cancels are not
    // counted. Ongoing and group-summary rows are excluded everywhere.

    /** Per-app alert totals and user-dismissed counts since [since], busiest first. */
    @Query("""
        SELECT packageName, MAX(appName) AS appName,
            COUNT(*) AS total,
            SUM(CASE WHEN removalReason IN ('CANCEL', 'CANCEL_ALL') OR userAction = 'ARCHIVED'
                OR (userAction = 'DISMISSED' AND removalReason IS NULL) THEN 1 ELSE 0 END) AS dismissed
        FROM notifications
        WHERE timestamp >= :since AND isOngoing = 0 AND isGroupSummary = 0
        GROUP BY packageName
        ORDER BY total DESC
    """)
    fun appAlertCountsFlow(since: Long): Flow<List<AppAlertCountRow>>

    /** Per-app counts bucketed by local calendar day (yyyy-MM-dd) since [since], for list sparklines. */
    @Query("""
        SELECT packageName,
            strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS day,
            COUNT(*) AS count
        FROM notifications
        WHERE timestamp >= :since AND isOngoing = 0 AND isGroupSummary = 0
        GROUP BY packageName, day
    """)
    fun appDailyCountsFlow(since: Long): Flow<List<AppDayCountRow>>

    /** Raw event times for one app since [since], oldest first: sparkline, hourly bars, time to act. */
    @Query("""
        SELECT timestamp, actionTimestamp, removedAt, channelId
        FROM notifications
        WHERE packageName = :packageName AND timestamp >= :since
        AND isOngoing = 0 AND isGroupSummary = 0
        ORDER BY timestamp ASC
    """)
    fun appEventsFlow(packageName: String, since: Long): Flow<List<AppEventRow>>

    /** Alert counts per notification channel for one app since [since]; channelId may be NULL. */
    @Query("""
        SELECT channelId, COUNT(*) AS count
        FROM notifications
        WHERE packageName = :packageName AND timestamp >= :since
        AND isOngoing = 0 AND isGroupSummary = 0
        GROUP BY channelId
        ORDER BY count DESC
    """)
    fun appChannelCountsFlow(packageName: String, since: Long): Flow<List<ChannelCountRow>>

    // -- Lanes / Screener (R014, R004) --

    /**
     * Per-app totals and dismissals within [quickMs] of posting since [since], for
     * re-screening prompts. Uses the same "dismissed" definition as [appAlertCountsFlow].
     */
    @Query("""
        SELECT packageName, MAX(appName) AS appName,
            COUNT(*) AS total,
            SUM(CASE WHEN (removalReason IN ('CANCEL', 'CANCEL_ALL') OR userAction = 'ARCHIVED'
                    OR (userAction = 'DISMISSED' AND removalReason IS NULL))
                AND COALESCE(removedAt, actionTimestamp) - timestamp >= 0
                AND COALESCE(removedAt, actionTimestamp) - timestamp < :quickMs
                THEN 1 ELSE 0 END) AS quickDismissed
        FROM notifications
        WHERE timestamp >= :since AND isOngoing = 0 AND isGroupSummary = 0
        GROUP BY packageName
    """)
    fun appQuickDismissStatsFlow(since: Long, quickMs: Long): Flow<List<AppQuickDismissRow>>

    /** Notification counts per (app, lane) since [since]; laneId is NULL for rows filed before v5. */
    @Query("""
        SELECT packageName, laneId, COUNT(*) AS count
        FROM notifications
        WHERE timestamp >= :since AND isOngoing = 0 AND isGroupSummary = 0
        GROUP BY packageName, laneId
    """)
    fun appLaneCountsFlow(since: Long): Flow<List<AppLaneCountRow>>

    /**
     * Recent rows no route claims (APP route for the package, CHANNEL on channelId, or
     * PERSON on COALESCE(NULLIF(shortcutId,''), title)), newest first: the Screener's
     * unscreened senders before grouping. Excludes [ownPackage] (Doorman itself).
     */
    @Query("""
        SELECT id, packageName, appName, title, text, timestamp, isConversation, shortcutId, channelId, laneId
        FROM notifications
        WHERE timestamp >= :since AND isOngoing = 0 AND isGroupSummary = 0 AND packageName != :ownPackage
        AND NOT EXISTS (
            SELECT 1 FROM routes r WHERE r.packageName = notifications.packageName AND (
                r.kind = 'APP' OR
                (r.kind = 'CHANNEL' AND r.`key` = notifications.channelId) OR
                (r.kind = 'PERSON' AND r.`key` = COALESCE(NULLIF(notifications.shortcutId, ''), notifications.title))
            )
        )
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun unroutedFlow(since: Long, ownPackage: String, limit: Int = 500): Flow<List<UnroutedRow>>

    // Hour-of-day distribution of dismissed notifications per app
    @Query("""
        SELECT packageName, appName,
            CAST(strftime('%H', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) as hour,
            COUNT(*) as count
        FROM notifications
        WHERE userAction = 'DISMISSED' AND isOngoing = 0 AND timestamp >= :sinceTimestamp
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
        WHERE isOngoing = 0 AND timestamp >= :sinceTimestamp
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
        WHERE isOngoing = 0 AND timestamp >= :sinceTimestamp
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
        WHERE isOngoing = 0 AND timestamp >= :sinceTimestamp
    """)
    suspend fun getActionBreakdown(sinceTimestamp: Long): ActionBreakdownRow
}

data class AppInfo(
    val packageName: String,
    val appName: String,
)

data class AppAlertCountRow(
    val packageName: String,
    val appName: String,
    val total: Int,
    val dismissed: Int,
)

data class AppQuickDismissRow(
    val packageName: String,
    val appName: String,
    val total: Int,
    val quickDismissed: Int,
)

data class AppLaneCountRow(
    val packageName: String,
    val laneId: Long?,
    val count: Int,
)

data class UnroutedRow(
    val id: Long,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val isConversation: Boolean,
    val shortcutId: String?,
    val channelId: String?,
    val laneId: Long?,
)

data class AppDayCountRow(
    val packageName: String,
    val day: String,
    val count: Int,
)

data class AppEventRow(
    val timestamp: Long,
    val actionTimestamp: Long?,
    val removedAt: Long?,
    val channelId: String?,
)

data class ChannelCountRow(
    val channelId: String?,
    val count: Int,
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
