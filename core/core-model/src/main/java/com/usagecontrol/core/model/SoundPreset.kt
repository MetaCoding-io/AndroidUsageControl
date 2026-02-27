package com.usagecontrol.core.model

data class SoundPreset(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val ringerMode: RingerMode = RingerMode.NORMAL,
    val alarmVolume: Int? = null,       // 0-100, null = don't change
    val ringVolume: Int? = null,
    val notificationVolume: Int? = null,
    val mediaVolume: Int? = null,
    val dndEnabled: Boolean? = null,    // null = don't change
    val triggerType: PresetTrigger,
    val triggerParams: Map<String, String> = emptyMap(),
)

enum class RingerMode {
    SILENT,
    VIBRATE,
    NORMAL,
}

enum class PresetTrigger {
    TIME_RANGE,     // params: "startTime", "endTime"
    WIFI_CONNECTED, // params: "ssid"
    MANUAL,         // user-activated only
}
