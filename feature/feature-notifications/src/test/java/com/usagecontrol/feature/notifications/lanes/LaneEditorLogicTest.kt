package com.usagecontrol.feature.notifications.lanes

import com.usagecontrol.core.model.DefaultLanes
import com.usagecontrol.core.model.LanePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaneEditorLogicTest {

    private val lanes = DefaultLanes.all
    private fun draft(name: String, id: Long = 0, decay: Int = 60) =
        LaneDraft(id = id, name = name, colorArgb = 0, decayMinutes = decay)

    @Test
    fun `valid new lane passes`() {
        assertNull(LaneEditorLogic.validate(draft("Shopping"), lanes))
    }

    @Test
    fun `blank, too long and duplicate names are rejected`() {
        assertEquals("Name is required", LaneEditorLogic.validate(draft("   "), lanes))
        assertTrue(LaneEditorLogic.validate(draft("x".repeat(25)), lanes)!!.contains("at most"))
        assertTrue(LaneEditorLogic.validate(draft(" quiet "), lanes)!!.contains("already exists"))
    }

    @Test
    fun `renaming a lane to its own name is not a duplicate`() {
        assertNull(LaneEditorLogic.validate(draft("Quiet", id = DefaultLanes.QUIET_ID), lanes))
    }

    @Test
    fun `decay out of range is rejected, boundaries accepted`() {
        assertTrue(LaneEditorLogic.validate(draft("A", decay = -1), lanes)!!.contains("Decay"))
        assertTrue(LaneEditorLogic.validate(draft("A", decay = LaneEditorLogic.MAX_DECAY_MINUTES + 1), lanes)!!.contains("Decay"))
        assertNull(LaneEditorLogic.validate(draft("A", decay = 0), lanes))
        assertNull(LaneEditorLogic.validate(draft("A", decay = LaneEditorLogic.MAX_DECAY_MINUTES), lanes))
    }

    @Test
    fun `breakthroughs round-trip through JSON including quotes and unicode`() {
        val items = listOf("Mum", "say \"hi\"", "back\\slash", "naïve ✓", "tab\there")
        val json = LaneEditorLogic.encodeBreakthroughs(items)
        assertEquals(items, LaneEditorLogic.decodeBreakthroughs(json))
        assertEquals("[]", LaneEditorLogic.encodeBreakthroughs(emptyList()))
        assertEquals(listOf("A"), LaneEditorLogic.decodeBreakthroughs("[\"\\u0041\"]"))
    }

    @Test
    fun `malformed breakthroughs JSON degrades to what parsed`() {
        assertTrue(LaneEditorLogic.decodeBreakthroughs(null).isEmpty())
        assertTrue(LaneEditorLogic.decodeBreakthroughs("").isEmpty())
        assertTrue(LaneEditorLogic.decodeBreakthroughs("{\"a\":1}").isEmpty())
        assertEquals(listOf("ok"), LaneEditorLogic.decodeBreakthroughs("[\"ok\", 42]"))
        assertEquals(listOf("ok"), LaneEditorLogic.decodeBreakthroughs("[\"ok\", \"unterminated]"))
        assertEquals(emptyList<String>(), LaneEditorLogic.decodeBreakthroughs("[\"\\u00G1\"]"))
    }

    @Test
    fun `parse breakthroughs trims, splits and de-duplicates case-insensitively`() {
        assertEquals(listOf("Mum", "boss", "urgent"), LaneEditorLogic.parseBreakthroughs(" Mum, boss,,\nurgent, mum "))
        assertTrue(LaneEditorLogic.parseBreakthroughs("  , ").isEmpty())
    }

    @Test
    fun `lane to draft and back preserves fields and blank sound becomes null`() {
        val quiet = lanes.first { it.id == DefaultLanes.QUIET_ID }
        val back = LaneEditorLogic.toLane(LaneEditorLogic.fromLane(quiet))
        assertEquals(quiet, back)
        val withSound = LaneEditorLogic.toLane(LaneDraft(name = " X ", colorArgb = 1, policy = LanePolicy.Digest, sound = "  "))
        assertNull(withSound.sound)
        assertEquals("X", withSound.name)
    }

    @Test
    fun `next sort order and decay formatting`() {
        assertEquals(6, LaneEditorLogic.nextSortOrder(lanes))
        assertEquals(0, LaneEditorLogic.nextSortOrder(emptyList()))
        assertEquals("Never", LaneEditorLogic.formatDecay(0))
        assertEquals("3 h", LaneEditorLogic.formatDecay(180))
        assertEquals("1 d", LaneEditorLogic.formatDecay(1440))
        assertEquals("45 min", LaneEditorLogic.formatDecay(45))
    }
}
