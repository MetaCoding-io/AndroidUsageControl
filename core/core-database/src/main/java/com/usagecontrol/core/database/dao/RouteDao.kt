package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.usagecontrol.core.database.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    /** Insert or replace the route for (kind, packageName, key). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: RouteEntity): Long

    @Query("DELETE FROM routes WHERE kind = :kind AND packageName = :packageName AND `key` = :key")
    suspend fun delete(kind: String, packageName: String, key: String): Int

    @Query("SELECT * FROM routes ORDER BY createdAt")
    fun getAllFlow(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes ORDER BY createdAt")
    suspend fun getAll(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE packageName = :packageName")
    suspend fun getForPackage(packageName: String): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE packageName = :packageName")
    fun getForPackageFlow(packageName: String): Flow<List<RouteEntity>>

    /**
     * Re-file an app's whole history into [laneId], leaving rows claimed by a more
     * specific CHANNEL or PERSON route for that app untouched.
     */
    @Query("""
        UPDATE notifications SET laneId = :laneId
        WHERE packageName = :packageName AND NOT EXISTS (
            SELECT 1 FROM routes r WHERE r.packageName = notifications.packageName AND (
                (r.kind = 'CHANNEL' AND r.`key` = notifications.channelId) OR
                (r.kind = 'PERSON' AND r.`key` = COALESCE(NULLIF(notifications.shortcutId, ''), notifications.title))
            )
        )
    """)
    suspend fun reFileApp(packageName: String, laneId: Long): Int

    /** Re-file one channel's history, leaving rows claimed by a PERSON route untouched. */
    @Query("""
        UPDATE notifications SET laneId = :laneId
        WHERE packageName = :packageName AND channelId = :channelId AND NOT EXISTS (
            SELECT 1 FROM routes r WHERE r.packageName = notifications.packageName
                AND r.kind = 'PERSON'
                AND r.`key` = COALESCE(NULLIF(notifications.shortcutId, ''), notifications.title)
        )
    """)
    suspend fun reFileChannel(packageName: String, channelId: String, laneId: Long): Int

    /** Re-file one person's history; key matches Route.personKeyOf(shortcutId, title). */
    @Query("""
        UPDATE notifications SET laneId = :laneId
        WHERE packageName = :packageName
            AND COALESCE(NULLIF(shortcutId, ''), title) = :personKey
    """)
    suspend fun reFilePerson(packageName: String, personKey: String, laneId: Long): Int

    /**
     * Re-file history matching a route. A null/empty [key] re-files the whole app;
     * otherwise [kind] selects channel vs person matching. Returns rows updated.
     */
    suspend fun reFileHistory(packageName: String, key: String?, laneId: Long, kind: String = "APP"): Int =
        when {
            key.isNullOrEmpty() || kind == "APP" -> reFileApp(packageName, laneId)
            kind == "CHANNEL" -> reFileChannel(packageName, key, laneId)
            else -> reFilePerson(packageName, key, laneId)
        }
}
