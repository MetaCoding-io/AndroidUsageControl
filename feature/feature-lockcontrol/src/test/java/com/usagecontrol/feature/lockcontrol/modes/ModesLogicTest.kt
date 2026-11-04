package com.usagecontrol.feature.lockcontrol.modes

import com.usagecontrol.core.common.SoundProfileApplier.ApplyResult
import com.usagecontrol.core.database.dao.DailyUsageTotalRow
import com.usagecontrol.core.model.PresetTrigger
import com.usagecontrol.core.model.RingerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModesLogicTest {

    private val presets = ModesLogic.defaultPresets().mapIndexed { i, p -> p.copy(id = i + 1L) }
    private val focus = presets.first { it.name == "Focus" }
    private val sleep = presets.first { it.name == "Sleep" }
    private val normal = presets.first { it.name == "Normal" }

    @Test
    fun defaultPresets_areManualFocusSleepNormal() {
        val defaults = ModesLogic.defaultPresets()
        assertEquals(listOf("Focus", "Sleep", "Normal"), defaults.map { it.name })
        assertTrue(defaults.all { it.triggerType == PresetTrigger.MANUAL })
        assertEquals(RingerMode.SILENT, defaults[1].ringerMode)
    }

    @Test
    fun enable_appliesTargetAndMarksActive() {
        val action = ModesLogic.resolveToggle(presets, sleep.id, enable = true)!!
        assertEquals(sleep, action.preset)
        assertEquals(sleep.id, action.activeId)
    }

    @Test
    fun disable_revertsToNormalAndClearsActive() {
        val action = ModesLogic.resolveToggle(presets, focus.id, enable = false)!!
        assertEquals(normal, action.preset)
        assertNull(action.activeId)
    }

    @Test
    fun disableNormal_appliesNothing() {
        val action = ModesLogic.resolveToggle(presets, normal.id, enable = false)!!
        assertNull(action.preset)
        assertNull(action.activeId)
    }

    @Test
    fun disable_withoutNormalPreset_appliesNothing() {
        val action = ModesLogic.resolveToggle(listOf(focus, sleep), focus.id, enable = false)!!
        assertNull(action.preset)
        assertNull(action.activeId)
    }

    @Test
    fun unknownPresetId_returnsNull() {
        assertNull(ModesLogic.resolveToggle(presets, 999L, enable = true))
        assertNull(ModesLogic.resolveToggle(emptyList(), 1L, enable = false))
    }

    @Test
    fun messageFor_onlyFailuresProduceText() {
        assertNull(ModesLogic.messageFor("Sleep", ApplyResult.Applied))
        assertTrue(ModesLogic.messageFor("Sleep", ApplyResult.PermissionDenied)!!.contains("Do Not Disturb"))
        assertTrue(ModesLogic.messageFor("Sleep", ApplyResult.Error("boom"))!!.contains("boom"))
    }

    @Test
    fun screenTimeSummary_emptyRows_isZero() {
        assertEquals(ScreenTimeSummary(), ModesLogic.screenTimeSummary(emptyList(), "2026-11-04"))
    }

    @Test
    fun screenTimeSummary_todayMissing_averagesExistingDays() {
        val rows = listOf(
            DailyUsageTotalRow("2026-11-02", 3_600_000, 5),
            DailyUsageTotalRow("2026-11-03", 1_800_000, 3),
        )
        val summary = ModesLogic.screenTimeSummary(rows, "2026-11-04")
        assertEquals(0L, summary.todayMs)
        assertEquals(2_700_000L, summary.avgDailyMs)
        assertEquals(2, summary.days)
    }

    @Test
    fun screenTimeSummary_picksToday() {
        val rows = listOf(
            DailyUsageTotalRow("2026-11-03", 1_000, 1),
            DailyUsageTotalRow("2026-11-04", 3_000, 2),
        )
        assertEquals(3_000L, ModesLogic.screenTimeSummary(rows, "2026-11-04").todayMs)
    }
}
