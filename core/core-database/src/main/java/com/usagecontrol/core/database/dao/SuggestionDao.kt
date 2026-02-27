package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.usagecontrol.core.database.entity.SuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestionDao {

    @Insert
    suspend fun insert(suggestion: SuggestionEntity): Long

    @Update
    suspend fun update(suggestion: SuggestionEntity)

    @Query("SELECT * FROM suggestions ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM suggestions WHERE status = 'PENDING' ORDER BY confidence DESC")
    fun getPendingFlow(): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM suggestions WHERE status = 'PENDING' ORDER BY confidence DESC")
    suspend fun getPending(): List<SuggestionEntity>

    @Query("SELECT * FROM suggestions WHERE id = :id")
    suspend fun getById(id: Long): SuggestionEntity?

    @Query("UPDATE suggestions SET status = :status, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, resolvedAt: Long = System.currentTimeMillis())

    @Query("UPDATE suggestions SET status = :status, resolvedAt = :resolvedAt, acceptedRuleId = :ruleId WHERE id = :id")
    suspend fun markAccepted(id: Long, ruleId: Long, status: String = "ACCEPTED", resolvedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM suggestions WHERE appPackageName = :packageName AND type = :type AND status = 'PENDING'")
    suspend fun countPending(packageName: String, type: String): Int

    @Query("DELETE FROM suggestions WHERE createdAt < :beforeTimestamp AND status != 'PENDING'")
    suspend fun deleteResolved(beforeTimestamp: Long)
}
