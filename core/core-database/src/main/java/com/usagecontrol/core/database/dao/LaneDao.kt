package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.usagecontrol.core.database.entity.LaneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LaneDao {

    @Insert
    suspend fun insert(lane: LaneEntity): Long

    @Update
    suspend fun update(lane: LaneEntity)

    /** Default lanes are seeded and cannot be deleted; returns rows removed. */
    @Query("DELETE FROM lanes WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustom(id: Long): Int

    @Query("SELECT * FROM lanes ORDER BY sortOrder, id")
    fun getAllFlow(): Flow<List<LaneEntity>>

    @Query("SELECT * FROM lanes ORDER BY sortOrder, id")
    suspend fun getAll(): List<LaneEntity>

    @Query("SELECT * FROM lanes WHERE id = :id")
    suspend fun getById(id: Long): LaneEntity?
}
