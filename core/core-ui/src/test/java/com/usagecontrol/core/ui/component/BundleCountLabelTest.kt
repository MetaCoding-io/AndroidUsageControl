package com.usagecontrol.core.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class BundleCountLabelTest {
    @Test
    fun negativeAndZeroClampToZero() {
        assertEquals("0", bundleCountLabel(-5))
        assertEquals("0", bundleCountLabel(0))
        assertEquals("0", bundleCountLabel(Int.MIN_VALUE))
    }

    @Test
    fun inRangeCountsAreLiteral() {
        assertEquals("1", bundleCountLabel(1))
        assertEquals("99", bundleCountLabel(99))
    }

    @Test
    fun overflowCapsAtNinetyNinePlus() {
        assertEquals("99+", bundleCountLabel(100))
        assertEquals("99+", bundleCountLabel(Int.MAX_VALUE))
    }
}
