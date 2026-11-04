package com.usagecontrol.core.database

import com.usagecontrol.core.database.dao.DismissStatsRow
import com.usagecontrol.core.database.entity.ContextSnapshotEntity
import com.usagecontrol.core.database.entity.NotificationEntity
import com.usagecontrol.core.database.entity.RuleEntity
import com.usagecontrol.core.database.entity.SoundPresetEntity
import com.usagecontrol.core.database.entity.SuggestionEntity
import com.usagecontrol.core.database.entity.UsageEntity
import com.usagecontrol.core.model.*
import org.json.JSONArray
import org.json.JSONObject

// -- Notification --

fun NotificationEntity.toModel() = NotificationRecord(
    id = id,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    subText = subText,
    category = category,
    priority = priority,
    timestamp = timestamp,
    groupKey = groupKey,
    isOngoing = isOngoing,
    userAction = NotificationAction.entries.firstOrNull { it.name == userAction } ?: NotificationAction.NONE,
    actionTimestamp = actionTimestamp,
    ruleIdIfAutoDismissed = ruleIdIfAutoDismissed,
    key = key,
    notifId = notifId,
    tag = tag,
    channelId = channelId,
    importance = importance,
    isConversation = isConversation,
    shortcutId = shortcutId,
    whenMs = whenMs,
    isGroupSummary = isGroupSummary,
    flags = flags,
    updateCount = updateCount,
    removalReason = removalReason,
    removedAt = removedAt,
    smallIconPath = smallIconPath,
    largeIconPath = largeIconPath,
    color = color,
    provisionalLane = provisionalLane,
    messagesJson = messagesJson,
    actionsJson = actionsJson,
)

fun NotificationRecord.toEntity() = NotificationEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    subText = subText,
    category = category,
    priority = priority,
    timestamp = timestamp,
    groupKey = groupKey,
    isOngoing = isOngoing,
    userAction = userAction.name,
    actionTimestamp = actionTimestamp,
    ruleIdIfAutoDismissed = ruleIdIfAutoDismissed,
    key = key,
    notifId = notifId,
    tag = tag,
    channelId = channelId,
    importance = importance,
    isConversation = isConversation,
    shortcutId = shortcutId,
    whenMs = whenMs,
    isGroupSummary = isGroupSummary,
    flags = flags,
    updateCount = updateCount,
    removalReason = removalReason,
    removedAt = removedAt,
    smallIconPath = smallIconPath,
    largeIconPath = largeIconPath,
    color = color,
    provisionalLane = provisionalLane,
    messagesJson = messagesJson,
    actionsJson = actionsJson,
)

// -- ContextSnapshot --

fun ContextSnapshotEntity.toModel() = ContextSnapshot(
    id = id,
    timestamp = timestamp,
    locationType = LocationType.valueOf(locationType),
    wifiSsid = wifiSsid,
    timeOfDay = timeOfDay,
    dayOfWeek = dayOfWeek,
    isCharging = isCharging,
    calendarEvent = calendarEvent,
    bluetoothDevice = bluetoothDevice,
)

// -- Usage --

fun UsageEntity.toModel() = UsageRecord(
    id = id,
    packageName = packageName,
    appName = appName,
    date = date,
    foregroundTimeMs = foregroundTimeMs,
    launchCount = launchCount,
    firstUsedTimestamp = firstUsedTimestamp,
    lastUsedTimestamp = lastUsedTimestamp,
)

fun UsageRecord.toEntity() = UsageEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    date = date,
    foregroundTimeMs = foregroundTimeMs,
    launchCount = launchCount,
    firstUsedTimestamp = firstUsedTimestamp,
    lastUsedTimestamp = lastUsedTimestamp,
)

// -- Rule --

fun RuleEntity.toModel() = Rule(
    id = id,
    name = name,
    enabled = enabled,
    priority = priority,
    pillar = RulePillar.valueOf(pillar),
    createdBy = RuleCreator.valueOf(createdBy),
    conditions = parseConditionsJson(conditionsJson),
    conditionGroups = parseConditionGroupsJson(conditionGroupsJson),
    actions = parseActionsJson(actionsJson),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Rule.toEntity() = RuleEntity(
    id = id,
    name = name,
    enabled = enabled,
    priority = priority,
    pillar = pillar.name,
    createdBy = createdBy.name,
    conditionsJson = conditionsToJson(conditions),
    conditionGroupsJson = conditionGroupsToJson(conditionGroups),
    actionsJson = actionsToJson(actions),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// -- SoundPreset --

fun SoundPresetEntity.toModel() = SoundPreset(
    id = id,
    name = name,
    enabled = enabled,
    ringerMode = RingerMode.valueOf(ringerMode),
    alarmVolume = alarmVolume,
    ringVolume = ringVolume,
    notificationVolume = notificationVolume,
    mediaVolume = mediaVolume,
    dndEnabled = dndEnabled,
    triggerType = PresetTrigger.valueOf(triggerType),
    triggerParams = parseMapJson(triggerParamsJson),
)

fun SoundPreset.toEntity() = SoundPresetEntity(
    id = id,
    name = name,
    enabled = enabled,
    ringerMode = ringerMode.name,
    alarmVolume = alarmVolume,
    ringVolume = ringVolume,
    notificationVolume = notificationVolume,
    mediaVolume = mediaVolume,
    dndEnabled = dndEnabled,
    triggerType = triggerType.name,
    triggerParamsJson = mapToJson(triggerParams),
)

// -- DismissStats --

fun DismissStatsRow.toModel() = AppDismissStats(
    packageName = packageName,
    appName = appName,
    totalNotifications = totalNotifications,
    dismissedCount = dismissedCount,
    openedCount = openedCount,
    autoDismissedCount = autoDismissedCount,
    avgTimeToActionMs = avgTimeToActionMs,
)

// -- Suggestion --

fun SuggestionEntity.toModel() = Suggestion(
    id = id,
    type = SuggestionType.valueOf(type),
    appPackageName = appPackageName,
    appName = appName,
    reason = reason,
    confidence = confidence,
    ruleTemplate = parseRuleJson(ruleTemplateJson),
    status = SuggestionStatus.valueOf(status),
    createdAt = createdAt,
    resolvedAt = resolvedAt,
    acceptedRuleId = acceptedRuleId,
)

fun Suggestion.toEntity() = SuggestionEntity(
    id = id,
    type = type.name,
    appPackageName = appPackageName,
    appName = appName,
    reason = reason,
    confidence = confidence,
    ruleTemplateJson = ruleToJson(ruleTemplate),
    status = status.name,
    createdAt = createdAt,
    resolvedAt = resolvedAt,
    acceptedRuleId = acceptedRuleId,
)

// -- JSON helpers --

private fun parseConditionsJson(json: String): List<RuleCondition> {
    if (json.isBlank()) return emptyList()
    val arr = JSONArray(json)
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        RuleCondition(
            type = ConditionType.valueOf(obj.getString("type")),
            params = parseMapJson(obj.optString("params", "{}")),
        )
    }
}

private fun conditionsToJson(conditions: List<RuleCondition>): String {
    val arr = JSONArray()
    conditions.forEach { c ->
        val obj = JSONObject()
        obj.put("type", c.type.name)
        obj.put("params", mapToJson(c.params))
        arr.put(obj)
    }
    return arr.toString()
}

private fun parseActionsJson(json: String): List<RuleAction> {
    if (json.isBlank()) return emptyList()
    val arr = JSONArray(json)
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        RuleAction(
            type = ActionType.valueOf(obj.getString("type")),
            params = parseMapJson(obj.optString("params", "{}")),
        )
    }
}

private fun actionsToJson(actions: List<RuleAction>): String {
    val arr = JSONArray()
    actions.forEach { a ->
        val obj = JSONObject()
        obj.put("type", a.type.name)
        obj.put("params", mapToJson(a.params))
        arr.put(obj)
    }
    return arr.toString()
}

private fun parseConditionGroupsJson(json: String): List<ConditionGroup> {
    if (json.isBlank() || json == "[]") return emptyList()
    val arr = JSONArray(json)
    return (0 until arr.length()).map { i ->
        val groupObj = arr.getJSONObject(i)
        val conditionsArr = groupObj.optJSONArray("conditions") ?: JSONArray()
        val conditions = (0 until conditionsArr.length()).map { j ->
            val condObj = conditionsArr.getJSONObject(j)
            RuleCondition(
                type = ConditionType.valueOf(condObj.getString("type")),
                params = parseMapJson(condObj.optString("params", "{}")),
            )
        }
        ConditionGroup(conditions = conditions)
    }
}

private fun conditionGroupsToJson(groups: List<ConditionGroup>): String {
    val arr = JSONArray()
    groups.forEach { group ->
        val groupObj = JSONObject()
        val conditionsArr = JSONArray()
        group.conditions.forEach { c ->
            val condObj = JSONObject()
            condObj.put("type", c.type.name)
            condObj.put("params", mapToJson(c.params))
            conditionsArr.put(condObj)
        }
        groupObj.put("conditions", conditionsArr)
        arr.put(groupObj)
    }
    return arr.toString()
}

private fun parseRuleJson(json: String): Rule {
    val obj = JSONObject(json)
    return Rule(
        name = obj.optString("name", ""),
        pillar = RulePillar.valueOf(obj.optString("pillar", "NOTIFICATION")),
        priority = obj.optInt("priority", 0),
        createdBy = RuleCreator.valueOf(obj.optString("createdBy", "LEARNED")),
        conditions = parseConditionsJson(obj.optString("conditions", "[]")),
        conditionGroups = parseConditionGroupsJson(obj.optString("conditionGroups", "[]")),
        actions = parseActionsJson(obj.optString("actions", "[]")),
    )
}

private fun ruleToJson(rule: Rule): String {
    val obj = JSONObject()
    obj.put("name", rule.name)
    obj.put("pillar", rule.pillar.name)
    obj.put("priority", rule.priority)
    obj.put("createdBy", rule.createdBy.name)
    obj.put("conditions", conditionsToJson(rule.conditions))
    obj.put("conditionGroups", conditionGroupsToJson(rule.conditionGroups))
    obj.put("actions", actionsToJson(rule.actions))
    return obj.toString()
}

private fun parseMapJson(json: String): Map<String, String> {
    if (json.isBlank()) return emptyMap()
    val obj = JSONObject(json)
    return obj.keys().asSequence().associateWith { obj.getString(it) }
}

private fun mapToJson(map: Map<String, String>): String {
    val obj = JSONObject()
    map.forEach { (k, v) -> obj.put(k, v) }
    return obj.toString()
}
