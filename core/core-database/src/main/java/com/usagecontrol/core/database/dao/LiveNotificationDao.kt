package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.usagecontrol.core.database.entity.LiveNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveNotificationDao {

    @Upsert
    suspend fun upsert(notification: LiveNotificationEntity)

    @Query("DELETE FROM live_notifications WHERE `key` = :key")
    suspend fun deleteByKey(key: String): Int

    @Query("SELECT * FROM live_notifications ORDER BY postTime DESC")
    fun getAllFlow(): Flow<List<LiveNotificationEntity>>

    @Query("SELECT `key` FROM live_notifications")
    suspend fun getAllKeys(): List<String>

    @Query("DELETE FROM live_notifications WHERE `key` NOT IN (:keys)")
    suspend fun deleteNotIn(keys: List<String>): Int

    @Query("DELETE FROM live_notifications")
    suspend fun clear()
}
