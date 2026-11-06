package com.usagecontrol.core.rules

import com.usagecontrol.core.model.DefaultLanes
import com.usagecontrol.core.model.Route
import com.usagecontrol.core.model.RouteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaneResolverTest {

    private val pkg = "com.chat"
    private val lanes = DefaultLanes.all

    private fun route(kind: RouteKind, laneId: Long, key: String = "", packageName: String = pkg) =
        Route(kind = kind, packageName = packageName, key = key, laneId = laneId, createdAt = 0)

    private fun resolve(
        routes: List<Route>,
        input: LaneInput = LaneInput(channelId = "chan"),
        personKey: String? = "alice",
        lanesArg: List<com.usagecontrol.core.model.Lane> = lanes,
    ) = LaneResolver.resolve(input, pkg, personKey, routes, lanesArg)

    private val all = listOf(
        route(RouteKind.APP, DefaultLanes.BATCHED_ID),
        route(RouteKind.CHANNEL, DefaultLanes.QUIET_ID, "chan"),
        route(RouteKind.PERSON, DefaultLanes.PEOPLE_ID, "alice"),
    )

    @Test fun personBeatsChannelAndApp() {
        val r = resolve(all)
        assertEquals(DefaultLanes.PEOPLE_ID, r.laneId)
        assertEquals(ResolutionSource.PERSON, r.source)
        assertFalse(r.needsScreening)
    }

    @Test fun channelBeatsAppWhenPersonDiffers() {
        val r = resolve(all, personKey = "bob")
        assertEquals(DefaultLanes.QUIET_ID, r.laneId)
        assertEquals(ResolutionSource.CHANNEL, r.source)
    }

    @Test fun appRouteWhenNoPersonOrChannelMatch() {
        val r = resolve(all, input = LaneInput(channelId = "other"), personKey = null)
        assertEquals(DefaultLanes.BATCHED_ID, r.laneId)
        assertEquals(ResolutionSource.APP, r.source)
        assertFalse(r.needsScreening)
    }

    @Test fun noRoutesFallsBackToCategoryDefaultAndNeedsScreening() {
        val r = resolve(emptyList(), input = LaneInput(category = ProvisionalLaneClassifier.CATEGORY_MESSAGE))
        assertEquals(DefaultLanes.PEOPLE_ID, r.laneId)
        assertEquals(ResolutionSource.DEFAULT, r.source)
        assertTrue(r.needsScreening)
    }

    @Test fun defaultMapsEachProvisionalLane() {
        assertEquals(DefaultLanes.INSTANT_ID, resolve(emptyList(), LaneInput(category = "alarm")).laneId)
        assertEquals(DefaultLanes.QUIET_ID, resolve(emptyList(), LaneInput(category = "promo")).laneId)
        assertEquals(DefaultLanes.BATCHED_ID, resolve(emptyList(), LaneInput()).laneId)
    }

    @Test fun routesForOtherPackagesAreIgnored() {
        val r = resolve(listOf(route(RouteKind.APP, DefaultLanes.BLOCKED_ID, packageName = "com.other")))
        assertEquals(ResolutionSource.DEFAULT, r.source)
        assertTrue(r.needsScreening)
    }

    @Test fun nullOrEmptyPersonKeyNeverMatchesPersonRoute() {
        val routes = listOf(route(RouteKind.PERSON, DefaultLanes.PEOPLE_ID, ""))
        assertEquals(ResolutionSource.DEFAULT, resolve(routes, personKey = null).source)
        assertEquals(ResolutionSource.DEFAULT, resolve(routes, personKey = "").source)
    }

    @Test fun nullChannelNeverMatchesChannelRoute() {
        val routes = listOf(route(RouteKind.CHANNEL, DefaultLanes.QUIET_ID, ""))
        assertEquals(ResolutionSource.DEFAULT, resolve(routes, input = LaneInput(channelId = null)).source)
    }

    @Test fun routeToDeletedLaneFallsThrough() {
        val routes = listOf(
            route(RouteKind.PERSON, 99L, "alice"),
            route(RouteKind.APP, DefaultLanes.DIGEST_ID),
        )
        val r = resolve(routes)
        assertEquals(DefaultLanes.DIGEST_ID, r.laneId)
        assertEquals(ResolutionSource.APP, r.source)
    }

    @Test fun missingDefaultLaneUsesFirstBySortOrder() {
        val withoutBatched = lanes.filter { it.id != DefaultLanes.BATCHED_ID }
        val r = resolve(emptyList(), input = LaneInput(), lanesArg = withoutBatched)
        assertEquals(DefaultLanes.INSTANT_ID, r.laneId)
        assertTrue(r.needsScreening)
    }

    @Test fun emptyLaneListSkipsExistenceChecks() {
        val r = resolve(listOf(route(RouteKind.APP, 42L)), lanesArg = emptyList())
        assertEquals(42L, r.laneId)
        assertEquals(ResolutionSource.APP, r.source)
    }
}
