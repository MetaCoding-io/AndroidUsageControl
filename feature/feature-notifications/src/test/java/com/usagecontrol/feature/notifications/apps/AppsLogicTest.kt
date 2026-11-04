package com.usagecontrol.feature.notifications.apps

import com.usagecontrol.core.database.dao.AppAlertCountRow
import com.usagecontrol.core.database.dao.AppDayCountRow
import com.usagecontrol.core.model.ActionType
import com.usagecontrol.core.model.ConditionType
import com.usagecontrol.core.model.Rule
import com.usagecontrol.core.model.RuleAction
import com.usagecontrol.core.model.RuleCondition
import com.usagecontrol.core.model.RulePillar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AppsLogicTest {

    private val today = LocalDate.of(2026, 11, 4)

    @Test
    fun buildRows_sortsByAlertsPerDayDescending_andBuildsSeries() {
        val rows = AppsLogic.buildRows(
            counts = listOf(
                AppAlertCountRow("a.quiet", "Quiet", total = 7, dismissed = 0),
                AppAlertCountRow("b.loud", "Loud", total = 70, dismissed = 35),
                AppAlertCountRow("c.mid", "Mid", total = 14, dismissed = 14),
            ),
            dayCounts = listOf(
                AppDayCountRow("b.loud", "2026-11-04", 10),
                AppDayCountRow("b.loud", "2026-10-29", 3),
                AppDayCountRow("b.loud", "2026-10-28", 99), // outside the 7-day window
            ),
            days = 7,
            today = today,
            labelOf = { it.appName },
        )
        assertEquals(listOf("b.loud", "c.mid", "a.quiet"), rows.map { it.packageName })
        assertEquals(10.0, rows[0].alertsPerDay, 1e-9)
        assertEquals(listOf(3, 0, 0, 0, 0, 0, 10), rows[0].series)
        assertEquals(50, rows[0].dismissPercent)
        assertEquals(100, rows[1].dismissPercent)
        assertEquals(List(7) { 0 }, rows[2].series)
    }

    @Test
    fun buildRows_tiesBreakByLabel_andEmptyInputGivesNoRows() {
        val rows = AppsLogic.buildRows(
            counts = listOf(AppAlertCountRow("z", "Zed", 7, 0), AppAlertCountRow("a", "Alpha", 7, 0)),
            dayCounts = emptyList(),
            days = 7,
            today = today,
            labelOf = { it.appName },
        )
        assertEquals(listOf("Alpha", "Zed"), rows.map { it.label })
        assertEquals(emptyList<AppRowUi>(), AppsLogic.buildRows(emptyList(), emptyList(), 7, today) { it.appName })
    }

    @Test
    fun buildRows_zeroTotalDoesNotDivideByZero() {
        val row = AppsLogic.buildRows(listOf(AppAlertCountRow("p", "P", 0, 0)), emptyList(), 7, today) { it.appName }
        assertEquals(0, row.single().dismissPercent)
    }

    @Test
    fun formatPerDay_dropsTrailingZero() {
        assertEquals("10/day", AppsLogic.formatPerDay(10.0))
        assertEquals("3.4/day", AppsLogic.formatPerDay(24.0 / 7))
        assertEquals("0/day", AppsLogic.formatPerDay(0.0))
    }

    @Test
    fun findLimitRule_matchesPackageAndPillar_newestWins() {
        val old = limitRule(id = 1, pkg = "com.x", minutes = 30, updatedAt = 100)
        val newer = limitRule(id = 2, pkg = "com.x", minutes = 45, updatedAt = 200)
        val otherApp = limitRule(id = 3, pkg = "com.y", minutes = 10, updatedAt = 300)
        val wrongPillar = limitRule(id = 4, pkg = "com.x", minutes = 5, updatedAt = 400)
            .copy(pillar = RulePillar.NOTIFICATION)
        val found = AppsLogic.findLimitRule(listOf(old, newer, otherApp, wrongPillar), "com.x")
        assertEquals(2L, found?.id)
        assertEquals(45, found?.let(AppsLogic::limitMinutes))
    }

    @Test
    fun findLimitRule_ignoresRulesWithoutUsableLimit() {
        val noLimit = limitRule(1, "com.x", 30, 1).copy(actions = listOf(RuleAction(type = ActionType.HARD_BLOCK)))
        val garbage = limitRule(2, "com.x", 30, 2).copy(
            actions = listOf(RuleAction(type = ActionType.ADVISE, params = mapOf("dailyLimitMinutes" to "abc"))),
        )
        val zero = limitRule(3, "com.x", 0, 3)
        assertNull(AppsLogic.findLimitRule(listOf(noLimit, garbage, zero), "com.x"))
        assertNull(AppsLogic.findLimitRule(emptyList(), "com.x"))
    }

    @Test
    fun newLimitRule_mirrorsScreenTimeShape() {
        val rule = AppsLogic.newLimitRule("Chat", "com.chat", 20)
        assertEquals("Chat: 20min/day", rule.name)
        assertEquals(RulePillar.SCREEN_TIME, rule.pillar)
        assertEquals(mapOf("packageName" to "com.chat"), rule.conditions.single().params)
        assertEquals(ActionType.ADVISE, rule.actions.single().type)
        assertEquals(20, AppsLogic.limitMinutes(rule))
        assertEquals(rule, AppsLogic.findLimitRule(listOf(rule), "com.chat"))
    }

    @Test
    fun withLimit_updatesOnlyLimitActions() {
        val rule = limitRule(7, "com.x", 30, 1).copy(
            actions = listOf(
                RuleAction(type = ActionType.SOFT_BLOCK, params = mapOf("dailyLimitMinutes" to "30")),
                RuleAction(type = ActionType.MUTE_NOTIFICATION),
            ),
        )
        val updated = AppsLogic.withLimit(rule, "X", 90, now = 999)
        assertEquals("90", updated.actions[0].params["dailyLimitMinutes"])
        assertEquals(ActionType.SOFT_BLOCK, updated.actions[0].type)
        assertEquals(rule.actions[1], updated.actions[1])
        assertEquals(999L, updated.updatedAt)
        assertEquals(7L, updated.id)
        assertEquals("X: 90min/day", updated.name)
    }

    @Test
    fun formatGap_coversUnits() {
        assertEquals("45s", formatGap(45_000))
        assertEquals("12m", formatGap(12 * 60_000L))
        assertEquals("3h 5m", formatGap((3 * 60 + 5) * 60_000L))
        assertEquals("0s", formatGap(0))
    }

    private fun limitRule(id: Long, pkg: String, minutes: Int, updatedAt: Long) = Rule(
        id = id,
        name = "r$id",
        pillar = RulePillar.SCREEN_TIME,
        conditions = listOf(RuleCondition(type = ConditionType.APP_PACKAGE, params = mapOf("packageName" to pkg))),
        actions = listOf(RuleAction(type = ActionType.ADVISE, params = mapOf("dailyLimitMinutes" to minutes.toString()))),
        updatedAt = updatedAt,
    )
}
