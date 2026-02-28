package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.usagecontrol.core.database.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Insert
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("SELECT * FROM rules ORDER BY priority DESC, createdAt DESC")
    fun getAllFlow(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules ORDER BY priority DESC, createdAt DESC")
    suspend fun getAll(): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE pillar = :pillar AND enabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledByPillar(pillar: String): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE pillar = :pillar ORDER BY priority DESC")
    fun getByPillarFlow(pillar: String): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: Long): RuleEntity?

    @Query("UPDATE rules SET enabled = :enabled, updatedAt = :timestamp WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
}
