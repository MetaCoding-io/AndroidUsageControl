package com.usagecontrol.core.model

data class Suggestion(
    val id: Long = 0,
    val type: SuggestionType,
    val appPackageName: String? = null,
    val appName: String? = null,
    val reason: String,
    val confidence: Float, // 0.0 to 1.0
    val ruleTemplate: Rule, // pre-built rule the user can accept
    val status: SuggestionStatus = SuggestionStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val acceptedRuleId: Long? = null,
)

enum class SuggestionType {
    AUTO_DISMISS_APP,          // High dismiss rate for an app
    TIME_BASED_DISMISS,        // Dismissals concentrated in time range
    QUICK_DISMISS_APP,         // Very fast dismiss times
    SCREEN_TIME_LIMIT,         // Heavy app usage pattern
}

enum class SuggestionStatus {
    PENDING,
    ACCEPTED,
    MODIFIED,  // Accepted with edits
    DISMISSED,
}
