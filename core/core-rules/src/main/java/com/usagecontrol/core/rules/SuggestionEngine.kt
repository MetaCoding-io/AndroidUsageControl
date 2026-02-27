package com.usagecontrol.core.rules

import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.SuggestionDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionEngine @Inject constructor(
    private val notificationDao: NotificationDao,
    private val usageDao: UsageDao,
    private val ruleDao: RuleDao,
    private val suggestionDao: SuggestionDao,
) {

    companion object {
        private const val MIN_NOTIFICATIONS = 10
        private const val HIGH_DISMISS_RATE = 0.90f
        private const val QUICK_DISMISS_MS = 3_000L  // 3 seconds avg = "immediate" dismiss
        private const val HEAVY_USAGE_MINUTES = 60    // 60min/day average
        private const val ANALYSIS_DAYS = 7L
        private const val HOUR_CONCENTRATION_THRESHOLD = 0.70f // 70% in a time window
    }

    /**
     * Analyze patterns and generate new suggestions.
     * Skips generating duplicates for already-pending suggestions.
     */
    suspend fun analyze(): List<Suggestion> {
        val since = System.currentTimeMillis() - (ANALYSIS_DAYS * 24 * 60 * 60 * 1000)
        val suggestions = mutableListOf<Suggestion>()

        suggestions.addAll(analyzeHighDismissRates(since))
        suggestions.addAll(analyzeQuickDismissals(since))
        suggestions.addAll(analyzeTimeBased(since))
        suggestions.addAll(analyzeHeavyUsage())

        // Save new suggestions to DB
        suggestions.forEach { suggestion ->
            suggestionDao.insert(suggestion.toEntity())
        }

        return suggestions
    }

    /**
     * Heuristic 1: High dismiss rate (>90% over 7 days with ≥10 notifications).
     * Suggests auto-dismiss rule for the app.
     */
    private suspend fun analyzeHighDismissRates(sinceTimestamp: Long): List<Suggestion> {
        val stats = notificationDao.getDismissStats(sinceTimestamp)
        val existingRulePackages = getExistingNotificationRulePackages()

        return stats.filter { row ->
            row.totalNotifications >= MIN_NOTIFICATIONS &&
                row.dismissedCount.toFloat() / row.totalNotifications >= HIGH_DISMISS_RATE &&
                row.packageName !in existingRulePackages
        }.mapNotNull { row ->
            val hasPending = suggestionDao.countPending(row.packageName, SuggestionType.AUTO_DISMISS_APP.name) > 0
            if (hasPending) return@mapNotNull null

            val rate = row.dismissedCount.toFloat() / row.totalNotifications
            Suggestion(
                type = SuggestionType.AUTO_DISMISS_APP,
                appPackageName = row.packageName,
                appName = row.appName,
                reason = "You dismiss ${(rate * 100).toInt()}% of ${row.appName} notifications (${row.dismissedCount}/${row.totalNotifications})",
                confidence = rate.coerceAtMost(1f),
                ruleTemplate = Rule(
                    name = "Auto-dismiss ${row.appName}",
                    pillar = RulePillar.NOTIFICATION,
                    createdBy = RuleCreator.LEARNED,
                    conditions = listOf(
                        RuleCondition(type = ConditionType.APP_PACKAGE, params = mapOf("packageName" to row.packageName))
                    ),
                    actions = listOf(RuleAction(type = ActionType.DISMISS_NOTIFICATION)),
                ),
            )
        }
    }

    /**
     * Heuristic 2: Very fast average dismiss time (<3s) indicates unwanted notifications.
     */
    private suspend fun analyzeQuickDismissals(sinceTimestamp: Long): List<Suggestion> {
        val stats = notificationDao.getDismissStats(sinceTimestamp)
        val existingRulePackages = getExistingNotificationRulePackages()

        return stats.filter { row ->
            row.totalNotifications >= MIN_NOTIFICATIONS &&
                row.avgTimeToActionMs != null &&
                row.avgTimeToActionMs < QUICK_DISMISS_MS &&
                row.dismissedCount > row.openedCount &&
                row.packageName !in existingRulePackages
        }.mapNotNull { row ->
            val hasPending = suggestionDao.countPending(row.packageName, SuggestionType.QUICK_DISMISS_APP.name) > 0
            if (hasPending) return@mapNotNull null

            val avgSeconds = (row.avgTimeToActionMs!! / 1000.0)
            Suggestion(
                type = SuggestionType.QUICK_DISMISS_APP,
                appPackageName = row.packageName,
                appName = row.appName,
                reason = "You dismiss ${row.appName} notifications in ${String.format("%.1f", avgSeconds)}s on average",
                confidence = (1f - (row.avgTimeToActionMs.toFloat() / QUICK_DISMISS_MS)).coerceIn(0.5f, 0.95f),
                ruleTemplate = Rule(
                    name = "Auto-dismiss ${row.appName}",
                    pillar = RulePillar.NOTIFICATION,
                    createdBy = RuleCreator.LEARNED,
                    conditions = listOf(
                        RuleCondition(type = ConditionType.APP_PACKAGE, params = mapOf("packageName" to row.packageName))
                    ),
                    actions = listOf(RuleAction(type = ActionType.DISMISS_NOTIFICATION)),
                ),
            )
        }
    }

    /**
     * Heuristic 3: Time-concentrated dismissals.
     * If ≥70% of dismissals for an app happen within a 4-hour window, suggest time-based rule.
     */
    private suspend fun analyzeTimeBased(sinceTimestamp: Long): List<Suggestion> {
        val hourlyData = notificationDao.getDismissalsByHour(sinceTimestamp)
        val existingRulePackages = getExistingNotificationRulePackages()

        // Group by package
        val byPackage = hourlyData.groupBy { it.packageName }

        return byPackage.mapNotNull { (packageName, rows) ->
            if (packageName in existingRulePackages) return@mapNotNull null

            val hasPending = suggestionDao.countPending(packageName, SuggestionType.TIME_BASED_DISMISS.name) > 0
            if (hasPending) return@mapNotNull null

            val totalDismissals = rows.sumOf { it.count }
            if (totalDismissals < MIN_NOTIFICATIONS) return@mapNotNull null

            // Sliding window: find best 4-hour window
            val hourCounts = IntArray(24)
            rows.forEach { hourCounts[it.hour] = it.count }

            var bestStart = 0
            var bestCount = 0
            for (start in 0..23) {
                val windowCount = (0..3).sumOf { hourCounts[(start + it) % 24] }
                if (windowCount > bestCount) {
                    bestCount = windowCount
                    bestStart = start
                }
            }

            val concentration = bestCount.toFloat() / totalDismissals
            if (concentration < HOUR_CONCENTRATION_THRESHOLD) return@mapNotNull null

            val startTime = String.format("%02d:00", bestStart)
            val endTime = String.format("%02d:00", (bestStart + 4) % 24)
            val appName = rows.first().appName

            Suggestion(
                type = SuggestionType.TIME_BASED_DISMISS,
                appPackageName = packageName,
                appName = appName,
                reason = "${(concentration * 100).toInt()}% of ${appName} dismissals happen between $startTime-$endTime",
                confidence = concentration.coerceIn(0.6f, 0.95f),
                ruleTemplate = Rule(
                    name = "Dismiss $appName $startTime-$endTime",
                    pillar = RulePillar.NOTIFICATION,
                    createdBy = RuleCreator.LEARNED,
                    conditionGroups = listOf(
                        ConditionGroup(
                            conditions = listOf(
                                RuleCondition(type = ConditionType.APP_PACKAGE, params = mapOf("packageName" to packageName)),
                                RuleCondition(type = ConditionType.TIME_RANGE, params = mapOf("startTime" to startTime, "endTime" to endTime)),
                            )
                        )
                    ),
                    actions = listOf(RuleAction(type = ActionType.DISMISS_NOTIFICATION)),
                ),
            )
        }
    }

    /**
     * Heuristic 4: Heavy app usage (>60min daily average).
     * Suggests advisory screen time limit.
     */
    private suspend fun analyzeHeavyUsage(): List<Suggestion> {
        val startDate = TimeUtils.dateOffset(-ANALYSIS_DAYS.toInt())
        val minAvgMs = HEAVY_USAGE_MINUTES * 60 * 1000L
        val heavyApps = usageDao.getHeavyUsageApps(startDate, minAvgMs)
        val existingScreenTimePackages = getExistingScreenTimeRulePackages()

        return heavyApps.mapNotNull { row ->
            if (row.packageName in existingScreenTimePackages) return@mapNotNull null

            val hasPending = suggestionDao.countPending(row.packageName, SuggestionType.SCREEN_TIME_LIMIT.name) > 0
            if (hasPending) return@mapNotNull null

            val avgMinutes = row.avgDailyMs / 60_000
            val suggestedLimit = (avgMinutes * 0.75).toInt().coerceAtLeast(15) // 25% reduction

            Suggestion(
                type = SuggestionType.SCREEN_TIME_LIMIT,
                appPackageName = row.packageName,
                appName = row.appName,
                reason = "You average ${avgMinutes}min/day on ${row.appName} over ${row.daysUsed} days",
                confidence = (avgMinutes.toFloat() / (HEAVY_USAGE_MINUTES * 3)).coerceIn(0.5f, 0.9f),
                ruleTemplate = Rule(
                    name = "${row.appName}: ${suggestedLimit}min/day",
                    pillar = RulePillar.SCREEN_TIME,
                    createdBy = RuleCreator.LEARNED,
                    conditions = listOf(
                        RuleCondition(type = ConditionType.APP_PACKAGE, params = mapOf("packageName" to row.packageName))
                    ),
                    actions = listOf(
                        RuleAction(type = ActionType.ADVISE, params = mapOf("dailyLimitMinutes" to suggestedLimit.toString()))
                    ),
                ),
            )
        }
    }

    private suspend fun getExistingNotificationRulePackages(): Set<String> {
        return ruleDao.getEnabledByPillar(RulePillar.NOTIFICATION.name)
            .map { it.toModel() }
            .flatMap { rule ->
                rule.conditions.filter { it.type == ConditionType.APP_PACKAGE }
                    .mapNotNull { it.params["packageName"] }
            }
            .toSet()
    }

    private suspend fun getExistingScreenTimeRulePackages(): Set<String> {
        return ruleDao.getEnabledByPillar(RulePillar.SCREEN_TIME.name)
            .map { it.toModel() }
            .flatMap { rule ->
                rule.conditions.filter { it.type == ConditionType.APP_PACKAGE }
                    .mapNotNull { it.params["packageName"] }
            }
            .toSet()
    }
}
