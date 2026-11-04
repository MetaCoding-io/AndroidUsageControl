package com.usagecontrol.feature.notifications.apps

import com.usagecontrol.core.database.dao.AppAlertCountRow
import com.usagecontrol.core.database.dao.AppDayCountRow
import com.usagecontrol.core.model.ActionType
import com.usagecontrol.core.model.ConditionType
import com.usagecontrol.core.model.Rule
import com.usagecontrol.core.model.RuleAction
import com.usagecontrol.core.model.RuleCondition
import com.usagecontrol.core.model.RulePillar
import java.time.LocalDate

/** One Apps-tab row. [series] is alerts per local day, oldest first. */
data class AppRowUi(
    val packageName: String,
    val label: String,
    val alertsPerDay: Double,
    val series: List<Int>,
    val dismissPercent: Int,
)

/** Pure builders for the Apps tab and the per-app screen-time limit rule. */
object AppsLogic {

    const val LIMIT_PARAM = "dailyLimitMinutes"

    /** Rows sorted by alerts/day descending (ties by label), with [days]-day sparklines. */
    fun buildRows(
        counts: List<AppAlertCountRow>,
        dayCounts: List<AppDayCountRow>,
        days: Int,
        today: LocalDate,
        labelOf: (AppAlertCountRow) -> String,
    ): List<AppRowUi> {
        val byPackage = dayCounts.groupBy { it.packageName }
        return counts.map { row ->
            val buckets = byPackage[row.packageName].orEmpty().associate { it.day to it.count }
            AppRowUi(
                packageName = row.packageName,
                label = labelOf(row),
                alertsPerDay = AppStats.alertsPerDay(row.total, days),
                series = AppStats.dailySeriesFromDayCounts(buckets, days, today),
                dismissPercent = (AppStats.dismissRate(row.dismissed, row.total) * 100).toInt(),
            )
        }.sortedWith(compareByDescending<AppRowUi> { it.alertsPerDay }.thenBy { it.label })
    }

    /** "3.4/day" style; whole numbers drop the decimal. */
    fun formatPerDay(value: Double): String {
        val rounded = Math.round(value * 10) / 10.0
        return if (rounded % 1.0 == 0.0) "${rounded.toLong()}/day" else "$rounded/day"
    }

    /**
     * The SCREEN_TIME rule that carries [packageName]'s daily limit: an APP_PACKAGE condition for
     * the package plus an action with a positive [LIMIT_PARAM]. Newest wins if there are several.
     */
    fun findLimitRule(rules: List<Rule>, packageName: String): Rule? =
        rules.filter { rule ->
            rule.pillar == RulePillar.SCREEN_TIME &&
                rule.conditions.any {
                    it.type == ConditionType.APP_PACKAGE && it.params["packageName"] == packageName
                } &&
                limitMinutes(rule) != null
        }.maxByOrNull { it.updatedAt }

    /** The rule's daily limit in minutes, or null when absent, unparseable or not positive. */
    fun limitMinutes(rule: Rule): Int? =
        rule.actions.firstNotNullOfOrNull { it.params[LIMIT_PARAM]?.toIntOrNull() }?.takeIf { it > 0 }

    /** Same shape as ScreenTimeViewModel.createTimeLimitRule. */
    fun newLimitRule(appName: String, packageName: String, minutes: Int): Rule = Rule(
        name = ruleName(appName, minutes),
        pillar = RulePillar.SCREEN_TIME,
        conditions = listOf(
            RuleCondition(type = ConditionType.APP_PACKAGE, params = mapOf("packageName" to packageName)),
        ),
        actions = listOf(
            RuleAction(type = ActionType.ADVISE, params = mapOf(LIMIT_PARAM to minutes.toString())),
        ),
    )

    /** [rule] with every limit-bearing action set to [minutes]; other actions are untouched. */
    fun withLimit(rule: Rule, appName: String, minutes: Int, now: Long): Rule = rule.copy(
        name = ruleName(appName, minutes),
        actions = rule.actions.map { action ->
            if (action.params.containsKey(LIMIT_PARAM)) {
                action.copy(params = action.params + (LIMIT_PARAM to minutes.toString()))
            } else {
                action
            }
        },
        updatedAt = now,
    )

    private fun ruleName(appName: String, minutes: Int) = "$appName: ${minutes}min/day"
}
