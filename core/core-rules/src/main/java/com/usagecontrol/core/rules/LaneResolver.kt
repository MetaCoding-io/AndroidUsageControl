package com.usagecontrol.core.rules

import com.usagecontrol.core.model.DefaultLanes
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.ProvisionalLane
import com.usagecontrol.core.model.Route
import com.usagecontrol.core.model.RouteKind

/** Which rule decided a notification's lane, in descending precedence. */
enum class ResolutionSource { PERSON, CHANNEL, APP, DEFAULT }

/**
 * Outcome of [LaneResolver.resolve]. [needsScreening] is true when no user route
 * matched, i.e. the sender has never been routed and should go through the Screener.
 */
data class Resolution(
    val laneId: Long,
    val source: ResolutionSource,
    val needsScreening: Boolean,
    /** The capture-time classification used for the DEFAULT fallback (always computed). */
    val provisional: ProvisionalLane,
)

/**
 * Pure lane routing (R014). Precedence: PERSON > CHANNEL > APP route > category default
 * (from [ProvisionalLaneClassifier] mapped via [DefaultLanes.forProvisional]).
 *
 * Routes pointing at a lane missing from [lanes] are ignored (stale after a lane delete),
 * so resolution falls through to the next precedence level instead of filing into nothing.
 * When [lanes] is empty no existence check is applied.
 */
object LaneResolver {

    fun resolve(
        input: LaneInput,
        packageName: String,
        personKey: String?,
        routes: List<Route>,
        lanes: List<Lane>,
    ): Resolution {
        val provisional = ProvisionalLaneClassifier.classify(input)
        val laneIds = lanes.mapTo(HashSet()) { it.id }
        fun live(route: Route?): Route? =
            route?.takeIf { laneIds.isEmpty() || it.laneId in laneIds }

        val appRoutes = routes.filter { it.packageName == packageName }

        if (!personKey.isNullOrEmpty()) {
            live(appRoutes.firstOrNull { it.kind == RouteKind.PERSON && it.key == personKey })
                ?.let { return Resolution(it.laneId, ResolutionSource.PERSON, false, provisional) }
        }
        if (!input.channelId.isNullOrEmpty()) {
            live(appRoutes.firstOrNull { it.kind == RouteKind.CHANNEL && it.key == input.channelId })
                ?.let { return Resolution(it.laneId, ResolutionSource.CHANNEL, false, provisional) }
        }
        live(appRoutes.firstOrNull { it.kind == RouteKind.APP })
            ?.let { return Resolution(it.laneId, ResolutionSource.APP, false, provisional) }

        return Resolution(defaultLaneId(provisional, lanes), ResolutionSource.DEFAULT, true, provisional)
    }

    /**
     * Seeded default lane for [provisional]; if the user removed it, the first lane by
     * sortOrder so the notification is still filed somewhere visible.
     */
    internal fun defaultLaneId(provisional: ProvisionalLane, lanes: List<Lane>): Long {
        val mapped = DefaultLanes.forProvisional(provisional)
        if (lanes.isEmpty() || lanes.any { it.id == mapped }) return mapped
        return lanes.minBy { it.sortOrder }.id
    }
}
