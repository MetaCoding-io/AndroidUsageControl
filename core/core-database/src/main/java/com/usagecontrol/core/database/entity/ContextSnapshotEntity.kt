package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_snapshots",
    indices = [Index("timestamp")]
)
data class ContextSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val locationType: String,
    val wifiSsid: String?,
    val timeOfDay: String?,
    val dayOfWeek: String?,
    val isCharging: Boolean,
    val calendarEvent: String?,
    val bluetoothDevice: String?,
)
