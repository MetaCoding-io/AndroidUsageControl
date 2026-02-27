package com.usagecontrol.core.model

data class UsageRecord(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val date: String, // yyyy-MM-dd
    val foregroundTimeMs: Long,
    val launchCount: Int,
    val firstUsedTimestamp: Long,
    val lastUsedTimestamp: Long,
)
