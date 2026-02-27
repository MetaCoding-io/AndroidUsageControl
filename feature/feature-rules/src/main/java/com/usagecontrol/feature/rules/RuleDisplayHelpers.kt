package com.usagecontrol.feature.rules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.usagecontrol.core.model.*

fun RulePillar.displayName(): String = when (this) {
    RulePillar.NOTIFICATION -> "Notification"
    RulePillar.SCREEN_TIME -> "Screen Time"
    RulePillar.LOCK_SOUND -> "Lock & Sound"
}

fun ConditionType.displayName(): String = when (this) {
    ConditionType.APP_PACKAGE -> "App"
    ConditionType.TIME_RANGE -> "Time Range"
    ConditionType.DAY_OF_WEEK -> "Day of Week"
    ConditionType.WIFI_SSID -> "WiFi Network"
    ConditionType.KEYWORD_MATCH -> "Keyword Match"
    ConditionType.NOTIFICATION_PRIORITY -> "Notification Priority"
    ConditionType.APP_CATEGORY -> "App Category"
    ConditionType.CHARGING -> "Charging State"
    ConditionType.BLUETOOTH_DEVICE -> "Bluetooth Device"
    ConditionType.CALENDAR_EVENT -> "Calendar Event"
}

fun ConditionType.icon(): ImageVector = when (this) {
    ConditionType.APP_PACKAGE -> Icons.Default.Phone
    ConditionType.TIME_RANGE -> Icons.Default.Schedule
    ConditionType.DAY_OF_WEEK -> Icons.Default.DateRange
    ConditionType.WIFI_SSID -> Icons.Default.Wifi
    ConditionType.KEYWORD_MATCH -> Icons.Default.Search
    ConditionType.NOTIFICATION_PRIORITY -> Icons.Default.PriorityHigh
    ConditionType.APP_CATEGORY -> Icons.Default.Category
    ConditionType.CHARGING -> Icons.Default.BatteryChargingFull
    ConditionType.BLUETOOTH_DEVICE -> Icons.Default.Bluetooth
    ConditionType.CALENDAR_EVENT -> Icons.Default.Event
}

fun RuleCondition.displayParams(): String = when (type) {
    ConditionType.APP_PACKAGE -> params["packageName"] ?: "—"
    ConditionType.TIME_RANGE -> "${params["startTime"] ?: "?"} – ${params["endTime"] ?: "?"}"
    ConditionType.DAY_OF_WEEK -> params["days"] ?: "—"
    ConditionType.WIFI_SSID -> params["ssid"] ?: "—"
    ConditionType.KEYWORD_MATCH -> "\"${params["keyword"] ?: ""}\" in ${params["field"] ?: "both"}"
    ConditionType.NOTIFICATION_PRIORITY -> "min: ${params["minPriority"] ?: "0"}"
    ConditionType.APP_CATEGORY -> params["category"] ?: "—"
    ConditionType.CHARGING -> if (params["isCharging"] == "true") "Charging" else "Not charging"
    ConditionType.BLUETOOTH_DEVICE -> params["deviceName"] ?: "—"
    ConditionType.CALENDAR_EVENT -> "\"${params["keyword"] ?: ""}\""
}

fun ActionType.displayName(): String = when (this) {
    ActionType.DISMISS_NOTIFICATION -> "Auto-dismiss"
    ActionType.MUTE_NOTIFICATION -> "Mute notification"
    ActionType.SNOOZE_NOTIFICATION -> "Snooze"
    ActionType.ADVISE -> "Advisory warning"
    ActionType.SOFT_BLOCK -> "Soft block"
    ActionType.HARD_BLOCK -> "Hard block"
    ActionType.SET_VOLUME -> "Set volume"
    ActionType.SET_RINGER_MODE -> "Set ringer mode"
    ActionType.SET_DND -> "Do Not Disturb"
    ActionType.SET_LOCK_TIMEOUT -> "Lock timeout"
}

fun ActionType.icon(): ImageVector = when (this) {
    ActionType.DISMISS_NOTIFICATION -> Icons.Default.NotificationsOff
    ActionType.MUTE_NOTIFICATION -> Icons.Default.VolumeOff
    ActionType.SNOOZE_NOTIFICATION -> Icons.Default.Snooze
    ActionType.ADVISE -> Icons.Default.Info
    ActionType.SOFT_BLOCK -> Icons.Default.Warning
    ActionType.HARD_BLOCK -> Icons.Default.Block
    ActionType.SET_VOLUME -> Icons.Default.VolumeUp
    ActionType.SET_RINGER_MODE -> Icons.Default.PhoneAndroid
    ActionType.SET_DND -> Icons.Default.DoNotDisturb
    ActionType.SET_LOCK_TIMEOUT -> Icons.Default.Lock
}

fun ActionType.forPillar(pillar: RulePillar): Boolean = when (pillar) {
    RulePillar.NOTIFICATION -> this in listOf(
        ActionType.DISMISS_NOTIFICATION,
        ActionType.MUTE_NOTIFICATION,
        ActionType.SNOOZE_NOTIFICATION,
    )
    RulePillar.SCREEN_TIME -> this in listOf(
        ActionType.ADVISE,
        ActionType.SOFT_BLOCK,
        ActionType.HARD_BLOCK,
    )
    RulePillar.LOCK_SOUND -> this in listOf(
        ActionType.SET_VOLUME,
        ActionType.SET_RINGER_MODE,
        ActionType.SET_DND,
        ActionType.SET_LOCK_TIMEOUT,
    )
}

fun RuleAction.displayParams(): String {
    if (params.isEmpty()) return ""
    return params.entries.joinToString(", ") { "${it.key}: ${it.value}" }
}

fun Rule.conditionSummary(): String {
    if (isCompound) {
        val groupSummaries = conditionGroups.map { group ->
            group.conditions.joinToString(" & ") { it.type.displayName() }
        }
        return groupSummaries.joinToString(" | ")
    }
    if (conditions.isEmpty()) return "Always"
    return conditions.joinToString(" & ") { it.type.displayName() }
}

fun Rule.actionSummary(): String {
    if (actions.isEmpty()) return "No actions"
    return actions.joinToString(", ") { it.type.displayName() }
}
