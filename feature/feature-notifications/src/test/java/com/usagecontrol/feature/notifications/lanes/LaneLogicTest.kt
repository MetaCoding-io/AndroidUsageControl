package com.usagecontrol.feature.notifications.lanes

import com.usagecontrol.core.database.dao.AppLaneCountRow
import com.usagecontrol.core.database.dao.AppQuickDismissRow
import com.usagecontrol.core.model.DefaultLanes
import com.usagecontrol.core.model.Route
import com.usagecontrol.core.model.RouteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaneLogicTest {

    private val lanes = DefaultLanes.all

    @Test
    fun `usual lane is the most frequent, ties to lower id, nulls ignored`() {
        val usual = LaneLogic.usualLanes(
            listOf(
                AppLaneCountRow("a", 3, 5), AppLaneCountRow("a", 4, 9), AppLaneCountRow("a", null, 50),
                AppLaneCountRow("b", 5, 2), AppLaneCountRow("b", 2, 2),
                AppLaneCountRow("c", null, 7),
            ),
        )
        assertEquals(mapOf("a" to 4L, "b" to 2L), usual)
    }

    @Test
    fun `only APP routes count as the app lane`() {
        val routes = listOf(
            Route(kind = RouteKind.PERSON, packageName = "chat", key = "Al", laneId = 2, createdAt = 0),
            Route(kind = RouteKind.APP, packageName = "shop", laneId = 4, createdAt = 0),
        )
        assertEquals(mapOf("shop" to 4L), LaneLogic.appRouteLanes(routes))
    }

    @Test
    fun `prompt fires for a noisy app filed in Batched`() {
        val prompts = LaneLogic.prompts(
            quickRows = listOf(AppQuickDismissRow("com.zalando", "Zalando", 42, 40)),
            routes = emptyList(),
            laneCounts = listOf(AppLaneCountRow("com.zalando", DefaultLanes.BATCHED_ID, 42)),
            lanes = lanes,
        )
        val p = prompts.single()
        assertEquals(DefaultLanes.QUIET_ID, p.targetLaneId)
        assertTrue(p.message, p.message.contains("40 of 42 from Zalando"))
    }

    @Test
    fun `no prompt when history already sits in Quiet, or the app is routed to Digest`() {
        val quick = listOf(AppQuickDismissRow("com.z", "Z", 30, 30))
        assertTrue(
            LaneLogic.prompts(quick, emptyList(), listOf(AppLaneCountRow("com.z", DefaultLanes.QUIET_ID, 30)), lanes).isEmpty(),
        )
        val routed = listOf(Route(kind = RouteKind.APP, packageName = "com.z", laneId = DefaultLanes.DIGEST_ID, createdAt = 0))
        assertTrue(
            LaneLogic.prompts(quick, routed, listOf(AppLaneCountRow("com.z", DefaultLanes.BATCHED_ID, 30)), lanes).isEmpty(),
        )
    }

    @Test
    fun `APP route beats usual history lane for the prompt decision`() {
        val quick = listOf(AppQuickDismissRow("com.z", "Z", 30, 30))
        val routed = listOf(Route(kind = RouteKind.APP, packageName = "com.z", laneId = DefaultLanes.INSTANT_ID, createdAt = 0))
        // History says Quiet (would suppress), but the user routed it to Instant afterwards.
        val prompts = LaneLogic.prompts(quick, routed, listOf(AppLaneCountRow("com.z", DefaultLanes.QUIET_ID, 30)), lanes)
        assertEquals(1, prompts.size)
    }

    @Test
    fun `dismissed prompts and insufficient samples are suppressed`() {
        val quick = listOf(AppQuickDismissRow("com.z", "Z", 30, 30), AppQuickDismissRow("com.y", "Y", 19, 19))
        assertTrue(LaneLogic.prompts(quick, emptyList(), emptyList(), lanes, dismissed = setOf("com.z")).isEmpty())
    }

    @Test
    fun `lane breakdown joins names, merges, drops unknown and other apps`() {
        val rows = listOf(
            AppLaneCountRow("a", 4, 3), AppLaneCountRow("a", 3, 7), AppLaneCountRow("a", 99, 100),
            AppLaneCountRow("a", null, 5), AppLaneCountRow("b", 4, 50),
        )
        val breakdown = LaneLogic.laneBreakdown("a", rows, lanes)
        assertEquals(listOf("Batched" to 7, "Quiet" to 3), breakdown.map { it.first.name to it.second })
    }
}
