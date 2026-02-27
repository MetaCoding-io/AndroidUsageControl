package com.usagecontrol.core.model

data class ContextSnapshot(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val locationType: LocationType = LocationType.UNKNOWN,
    val wifiSsid: String? = null,
    val timeOfDay: String? = null, // HH:mm
    val dayOfWeek: String? = null, // MON, TUE, etc.
    val isCharging: Boolean = false,
    val calendarEvent: String? = null,
    val bluetoothDevice: String? = null,
)

enum class LocationType {
    HOME,
    WORK,
    OTHER,
    UNKNOWN,
}
