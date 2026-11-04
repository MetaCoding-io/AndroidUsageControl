package com.usagecontrol.feature.notifications

import com.usagecontrol.core.model.NotificationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIconPathTest {

    private val hash = "a".repeat(64)
    private val good = "icons/com.example.app/$hash.png"

    private fun record(small: String? = null, large: String? = null) = NotificationRecord(
        packageName = "com.example.app",
        appName = "Example",
        title = null,
        text = null,
        subText = null,
        category = null,
        priority = 0,
        timestamp = 0,
        groupKey = null,
        isOngoing = false,
        smallIconPath = small,
        largeIconPath = large,
    )

    @Test
    fun `accepts a well-formed stored path`() {
        assertEquals(good, safeIconPath(good))
    }

    @Test
    fun `rejects null, blank and traversal paths`() {
        assertNull(safeIconPath(null))
        assertNull(safeIconPath(""))
        assertNull(safeIconPath("icons/../databases/$hash.png"))
        assertNull(safeIconPath("icons/com.example..app/$hash.png"))
        assertNull(safeIconPath("../icons/com.example.app/$hash.png"))
    }

    @Test
    fun `rejects paths outside the icon store or with the wrong shape`() {
        assertNull(safeIconPath("/data/data/x/files/icons/com.example.app/$hash.png"))
        assertNull(safeIconPath("other/com.example.app/$hash.png"))
        assertNull(safeIconPath("icons/$hash.png"))
        assertNull(safeIconPath("icons/com.example.app/short.png"))
        assertNull(safeIconPath("icons/com.example.app/$hash.jpg"))
        assertNull(safeIconPath("icons/com.example.app/sub/$hash.png"))
        assertNull(safeIconPath("icons/com.example.app/${"A".repeat(64)}.png"))
    }

    @Test
    fun `prefers the large icon untinted, then the small icon tinted`() {
        val small = "icons/com.example.app/${"b".repeat(64)}.png"
        assertEquals(IconSource(good, tint = false), iconSource(record(small = small, large = good)))
        assertEquals(IconSource(small, tint = true), iconSource(record(small = small)))
    }

    @Test
    fun `unsafe paths fall back to the app icon`() {
        val source = iconSource(record(small = "../x", large = "icons/../y"))
        assertNull(source.path)
        assertFalse(source.tint)
        assertTrue(iconSource(record()).path == null)
    }
}
