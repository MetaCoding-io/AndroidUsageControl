package com.usagecontrol.core.model

data class Rule(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val pillar: RulePillar,
    val createdBy: RuleCreator = RuleCreator.USER,
    val conditions: List<RuleCondition> = emptyList(),
    val conditionGroups: List<ConditionGroup> = emptyList(),
    val actions: List<RuleAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /**
     * Whether this rule uses compound condition groups (OR between groups).
     * If conditionGroups is empty, the legacy flat conditions list is used (all AND).
     */
    val isCompound: Boolean get() = conditionGroups.isNotEmpty()
}

enum class RulePillar {
    NOTIFICATION,
    SCREEN_TIME,
    LOCK_SOUND,
}

enum class RuleCreator {
    USER,
    SUGGESTION,
    LEARNED,
}
