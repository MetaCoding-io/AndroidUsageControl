package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.usagecontrol.core.database.entity.ContextSnapshotEntity

@Dao
interface ContextSnapshotDao {

    @Insert
    suspend fun insert(snapshot: ContextSnapshotEntity): Long

    @Query("SELECT * FROM context_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ContextSnapshotEntity?

    @Query("DELETE FROM context_snapshots WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int
}
