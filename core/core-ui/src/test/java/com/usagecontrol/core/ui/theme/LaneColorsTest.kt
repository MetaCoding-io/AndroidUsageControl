package com.usagecontrol.core.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaneColorsTest {

    @Test
    fun everyLaneDiffersBetweenDarkAndLight() {
        Lane.entries.forEach { lane ->
            assertNotEquals("$lane should differ between themes", DarkLaneColors[lane], LightLaneColors[lane])
        }
    }

    @Test
    fun allSixDarkLanesAreDistinct() {
        assertEquals(6, DarkLaneColors.all.size)
        assertEquals(6, DarkLaneColors.all.toSet().size)
    }

    @Test
    fun allSixLightLanesAreDistinct() {
        assertEquals(6, LightLaneColors.all.size)
        assertEquals(6, LightLaneColors.all.toSet().size)
    }

    @Test
    fun laneColorsSelectsByTheme() {
        assertEquals(DarkLaneColors, laneColors(darkTheme = true))
        assertEquals(LightLaneColors, laneColors(darkTheme = false))
    }

    @Test
    fun lightLanesAreLessSaturatedThanDark() {
        Lane.entries.forEach { lane ->
            val dark = saturation(DarkLaneColors[lane].red, DarkLaneColors[lane].green, DarkLaneColors[lane].blue)
            val light = saturation(LightLaneColors[lane].red, LightLaneColors[lane].green, LightLaneColors[lane].blue)
            assertTrue("$lane light saturation $light should not exceed dark $dark", light <= dark)
        }
    }

    private fun saturation(r: Float, g: Float, b: Float): Float {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        return if (max == 0f) 0f else (max - min) / max
    }
}
