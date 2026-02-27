package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val priority: Int,
    val pillar: String,   // RulePillar name
    val createdBy: String, // RuleCreator name
    val conditionsJson: String,        // JSON-serialized List<RuleCondition>
    val conditionGroupsJson: String = "[]", // JSON-serialized List<ConditionGroup>
    val actionsJson: String,           // JSON-serialized List<RuleAction>
    val createdAt: Long,
    val updatedAt: Long,
)
