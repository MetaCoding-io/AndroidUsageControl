package com.usagecontrol.feature.notifications.lanes

import com.usagecontrol.core.database.dao.AppLaneCountRow
import com.usagecontrol.core.database.dao.AppQuickDismissRow
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.Route
import com.usagecontrol.core.model.RouteKind
import com.usagecontrol.core.rules.AppDismissalStats
import com.usagecontrol.core.rules.ReScreenPrompt
import com.usagecontrol.core.rules.ReScreenPromptEngine

/** Pure lane helpers shared by the Screener sheet and the app profile. */
object LaneLogic {

    /** Days of history the Screener and re-screen prompts look at. */
    const val WINDOW_DAYS = 14

    /** The APP route's lane per package (person/channel routes do not move the whole app). */
    fun appRouteLanes(routes: List<Route>): Map<String, Long> =
        routes.filter { it.kind == RouteKind.APP }.associate { it.packageName to it.laneId }

    /** Most frequent lane per package in its history; ties go to the lower lane id. NULL lanes are ignored. */
    fun usualLanes(rows: List<AppLaneCountRow>): Map<String, Long> =
        rows.filter { it.laneId != null && it.count > 0 }
            .groupBy { it.packageName }
            .mapValues { (_, group) ->
                group.sortedWith(compareByDescending<AppLaneCountRow> { it.count }.thenBy { it.laneId }).first().laneId!!
            }

    /**
     * Re-screening prompts (R004) for every app with enough history. An app's current lane is
     * its APP route, else the lane most of its history sits in, so an app the classifier
     * already files as Quiet is not asked to move to Quiet.
     */
    fun prompts(
        quickRows: List<AppQuickDismissRow>,
        routes: List<Route>,
        laneCounts: List<AppLaneCountRow>,
        lanes: List<Lane>,
        dismissed: Set<String> = emptySet(),
    ): List<ReScreenPrompt> {
        val routed = appRouteLanes(routes)
        val usual = usualLanes(laneCounts)
        val stats = quickRows.filter { it.packageName !in dismissed }.map { row ->
            AppDismissalStats(
                packageName = row.packageName,
                appName = row.appName.ifBlank { row.packageName },
                total = row.total,
                quickDismissed = row.quickDismissed,
                currentLaneId = routed[row.packageName] ?: usual[row.packageName],
            )
        }
        return ReScreenPromptEngine.prompts(stats, lanes)
    }

    /** History per lane for one package, busiest first, joined to lane names; unknown/NULL lanes dropped. */
    fun laneBreakdown(packageName: String, rows: List<AppLaneCountRow>, lanes: List<Lane>): List<Pair<Lane, Int>> {
        val byId = lanes.associateBy { it.id }
        return rows.filter { it.packageName == packageName }
            .mapNotNull { row -> row.laneId?.let(byId::get)?.let { it to row.count } }
            .groupBy({ it.first }, { it.second })
            .map { (lane, counts) -> lane to counts.sum() }
            .sortedWith(compareByDescending<Pair<Lane, Int>> { it.second }.thenBy { it.first.sortOrder })
    }
}
