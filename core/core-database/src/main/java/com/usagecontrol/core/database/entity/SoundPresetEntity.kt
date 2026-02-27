package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sound_presets")
data class SoundPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val ringerMode: String,
    val alarmVolume: Int?,
    val ringVolume: Int?,
    val notificationVolume: Int?,
    val mediaVolume: Int?,
    val dndEnabled: Boolean?,
    val triggerType: String,
    val triggerParamsJson: String,
)
