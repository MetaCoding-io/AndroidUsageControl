package com.usagecontrol.core.model

/**
 * A group of conditions that are evaluated with AND logic internally.
 * Multiple ConditionGroups within a rule are evaluated with OR logic between them.
 *
 * Example: (App=Twitter AND Time=9-5) OR (App=Facebook AND Day=Weekend)
 */
data class ConditionGroup(
    val id: Long = 0,
    val conditions: List<RuleCondition> = emptyList(),
)
