package com.usagecontrol.core.model

data class RuleCondition(
    val id: Long = 0,
    val type: ConditionType,
    val params: Map<String, String> = emptyMap(),
)

enum class ConditionType {
    APP_PACKAGE,        // params: "packageName" -> "com.example.app"
    TIME_RANGE,         // params: "startTime" -> "22:00", "endTime" -> "07:00"
    DAY_OF_WEEK,        // params: "days" -> "MON,TUE,WED,THU,FRI"
    WIFI_SSID,          // params: "ssid" -> "MyHomeWifi"
    KEYWORD_MATCH,      // params: "keyword" -> "sale", "field" -> "title|text|both"
    NOTIFICATION_PRIORITY, // params: "minPriority" -> "0"
    APP_CATEGORY,       // params: "category" -> "social|game|productivity"
    CHARGING,           // params: "isCharging" -> "true"
    BLUETOOTH_DEVICE,   // params: "deviceName" -> "Car Stereo"
    CALENDAR_EVENT,     // params: "keyword" -> "meeting"
}
