package com.usagecontrol.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suggestions")
data class SuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,              // SuggestionType name
    val appPackageName: String?,
    val appName: String?,
    val reason: String,
    val confidence: Float,
    val ruleTemplateJson: String,  // JSON-serialized Rule
    val status: String,            // SuggestionStatus name
    val createdAt: Long,
    val resolvedAt: Long?,
    val acceptedRuleId: Long?,
)
