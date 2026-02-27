package com.usagecontrol.core.rules

import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.model.ConditionType
import com.usagecontrol.core.model.ContextSnapshot
import com.usagecontrol.core.model.RuleCondition
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConditionEvaluator @Inject constructor() {

    /**
     * Evaluate a single condition against the current context and optional notification data.
     */
    fun evaluate(
        condition: RuleCondition,
        context: EvaluationContext,
    ): Boolean {
        return when (condition.type) {
            ConditionType.APP_PACKAGE -> {
                val target = condition.params["packageName"] ?: return false
                context.packageName == target
            }

            ConditionType.TIME_RANGE -> {
                val start = condition.params["startTime"] ?: return false
                val end = condition.params["endTime"] ?: return false
                val current = context.snapshot?.timeOfDay ?: TimeUtils.currentTimeOfDay()
                TimeUtils.isTimeInRange(current, start, end)
            }

            ConditionType.DAY_OF_WEEK -> {
                val days = condition.params["days"]?.split(",")?.map { it.trim() } ?: return false
                val current = context.snapshot?.dayOfWeek ?: TimeUtils.currentDayOfWeek()
                current in days
            }

            ConditionType.WIFI_SSID -> {
                val targetSsid = condition.params["ssid"] ?: return false
                context.snapshot?.wifiSsid == targetSsid
            }

            ConditionType.KEYWORD_MATCH -> {
                val keyword = condition.params["keyword"]?.lowercase() ?: return false
                val field = condition.params["field"] ?: "both"
                val titleMatch = context.notificationTitle?.lowercase()?.contains(keyword) == true
                val textMatch = context.notificationText?.lowercase()?.contains(keyword) == true
                when (field) {
                    "title" -> titleMatch
                    "text" -> textMatch
                    else -> titleMatch || textMatch
                }
            }

            ConditionType.NOTIFICATION_PRIORITY -> {
                val minPriority = condition.params["minPriority"]?.toIntOrNull() ?: return false
                (context.notificationPriority ?: 0) >= minPriority
            }

            ConditionType.APP_CATEGORY -> {
                val target = condition.params["category"] ?: return false
                context.appCategory == target
            }

            ConditionType.CHARGING -> {
                val expected = condition.params["isCharging"]?.toBoolean() ?: return false
                context.snapshot?.isCharging == expected
            }

            ConditionType.BLUETOOTH_DEVICE -> {
                val target = condition.params["deviceName"] ?: return false
                context.snapshot?.bluetoothDevice == target
            }

            ConditionType.CALENDAR_EVENT -> {
                val keyword = condition.params["keyword"]?.lowercase() ?: return false
                context.snapshot?.calendarEvent?.lowercase()?.contains(keyword) == true
            }
        }
    }

    /**
     * Evaluate all conditions for a rule (AND logic — all must match).
     */
    fun evaluateAll(
        conditions: List<RuleCondition>,
        context: EvaluationContext,
    ): Boolean {
        if (conditions.isEmpty()) return true
        return conditions.all { evaluate(it, context) }
    }
}

data class EvaluationContext(
    val packageName: String? = null,
    val notificationTitle: String? = null,
    val notificationText: String? = null,
    val notificationPriority: Int? = null,
    val appCategory: String? = null,
    val snapshot: ContextSnapshot? = null,
)
