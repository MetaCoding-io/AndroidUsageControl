package com.usagecontrol.service.notification

import com.usagecontrol.core.model.DefaultLanes
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.LanePolicy
import com.usagecontrol.core.model.RouteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenerMappingTest {

    private val own = "com.usagecontrol"

    @Test
    fun targetFor_conversationWithPerson_isPersonRoute() {
        val t = ScreenerMapping.targetFor("com.whatsapp", isConversation = true, personKey = "alice")
        assertEquals(ScreenerTarget(RouteKind.PERSON, "com.whatsapp", "alice"), t)
    }

    @Test
    fun targetFor_conversationWithoutPersonKey_fallsBackToApp() {
        assertEquals(RouteKind.APP, ScreenerMapping.targetFor("com.whatsapp", true, null).kind)
        assertEquals(RouteKind.APP, ScreenerMapping.targetFor("com.whatsapp", true, "").kind)
    }

    @Test
    fun targetFor_nonConversation_isAppRouteWithEmptyKey() {
        val t = ScreenerMapping.targetFor("com.zalando", isConversation = false, personKey = "Sale!")
        assertEquals(ScreenerTarget(RouteKind.APP, "com.zalando", ""), t)
    }

    @Test
    fun shouldScreen_unroutedInteractivePost() {
        assertTrue(ScreenerMapping.shouldScreen(true, "com.zalando", own, isGroupSummary = false, interactive = true))
    }

    @Test
    fun shouldScreen_rejectsRoutedOwnSummaryAndReconcile() {
        assertFalse(ScreenerMapping.shouldScreen(false, "com.zalando", own, false, true))
        assertFalse(ScreenerMapping.shouldScreen(true, own, own, false, true))
        assertFalse(ScreenerMapping.shouldScreen(true, "com.zalando", own, isGroupSummary = true, interactive = true))
        assertFalse(ScreenerMapping.shouldScreen(true, "com.zalando", own, isGroupSummary = false, interactive = false))
    }

    @Test
    fun isOwnScreenerPost_onlyOwnPackageOnScreenerChannel() {
        assertTrue(ScreenerMapping.isOwnScreenerPost(own, ScreenerMapping.CHANNEL_ID, own))
        assertFalse(ScreenerMapping.isOwnScreenerPost(own, "usage_monitor", own))
        assertFalse(ScreenerMapping.isOwnScreenerPost("com.other", ScreenerMapping.CHANNEL_ID, own))
        assertFalse(ScreenerMapping.isOwnScreenerPost(own, null, own))
    }

    @Test
    fun actionLanes_suggestedFirstThenSortOrder_cappedAtThree() {
        val ids = ScreenerMapping.actionLanes(DefaultLanes.all, DefaultLanes.QUIET_ID).map { it.id }
        assertEquals(listOf(DefaultLanes.QUIET_ID, DefaultLanes.INSTANT_ID, DefaultLanes.PEOPLE_ID), ids)
    }

    @Test
    fun actionLanes_unknownSuggestion_usesSortOrder() {
        val ids = ScreenerMapping.actionLanes(DefaultLanes.all, 999L).map { it.id }
        assertEquals(listOf(DefaultLanes.INSTANT_ID, DefaultLanes.PEOPLE_ID, DefaultLanes.BATCHED_ID), ids)
    }

    @Test
    fun actionLanes_emptyOrFewLanesOrZeroMax() {
        assertTrue(ScreenerMapping.actionLanes(emptyList(), DefaultLanes.INSTANT_ID).isEmpty())
        val one = listOf(Lane(7, "Mine", 0, LanePolicy.Quiet, 0, sortOrder = 9))
        assertEquals(listOf(7L), ScreenerMapping.actionLanes(one, 1L).map { it.id })
        assertTrue(ScreenerMapping.actionLanes(DefaultLanes.all, 1L, max = 0).isEmpty())
    }

    @Test
    fun notificationIdFor_stablePositiveAndDistinctPerTarget() {
        val a = ScreenerTarget(RouteKind.APP, "com.a", "")
        val b = ScreenerTarget(RouteKind.PERSON, "com.a", "bob")
        assertEquals(ScreenerMapping.notificationIdFor(a), ScreenerMapping.notificationIdFor(a.copy()))
        assertNotEquals(ScreenerMapping.notificationIdFor(a), ScreenerMapping.notificationIdFor(b))
        assertTrue(ScreenerMapping.notificationIdFor(a) > 0)
        assertTrue(ScreenerMapping.notificationIdFor(ScreenerTarget(RouteKind.PERSON, "x", "￿".repeat(50))) > 0)
    }

    @Test
    fun dedupeKey_separatesKindPackageAndKey() {
        assertNotEquals(
            ScreenerTarget(RouteKind.APP, "com.a", "").dedupeKey,
            ScreenerTarget(RouteKind.PERSON, "com.a", "").dedupeKey,
        )
        assertEquals("PERSON|com.a|bob", ScreenerTarget(RouteKind.PERSON, "com.a", "bob").dedupeKey)
    }
}
