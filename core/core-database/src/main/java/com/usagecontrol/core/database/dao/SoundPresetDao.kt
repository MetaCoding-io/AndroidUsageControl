package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.usagecontrol.core.database.entity.SoundPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundPresetDao {

    @Insert
    suspend fun insert(preset: SoundPresetEntity): Long

    @Update
    suspend fun update(preset: SoundPresetEntity)

    @Delete
    suspend fun delete(preset: SoundPresetEntity)

    @Query("SELECT * FROM sound_presets ORDER BY name")
    fun getAllFlow(): Flow<List<SoundPresetEntity>>

    @Query("SELECT * FROM sound_presets WHERE enabled = 1")
    suspend fun getEnabled(): List<SoundPresetEntity>

    @Query("SELECT * FROM sound_presets WHERE id = :id")
    suspend fun getById(id: Long): SoundPresetEntity?
}
