package com.usagecontrol.service.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconStorePathTest {

    private val hash = IconStore.sha256("icon".toByteArray())

    @Test
    fun sha256_isLowercaseHex64() {
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(hash, IconStore.sha256("icon".toByteArray()))
    }

    @Test
    fun relativePath_validPackage() {
        assertEquals("icons/com.example.app/$hash.png", IconStore.relativePath("com.example.app", hash))
    }

    @Test
    fun relativePath_rejectsTraversalAndSeparators() {
        assertNull(IconStore.relativePath("..", hash))
        assertNull(IconStore.relativePath("../evil", hash))
        assertNull(IconStore.relativePath("com/evil", hash))
        assertNull(IconStore.relativePath("", hash))
        assertNull(IconStore.relativePath("com..evil", hash))
        assertNull(IconStore.relativePath("com.example", "../x"))
    }

    @Test
    fun relativePath_rejectsOverlongPackage() {
        assertNull(IconStore.relativePath("a".repeat(256), hash))
    }

    @Test
    fun isSafeRelativePath_rejectsForeignPaths() {
        assertFalse(IconStore.isSafeRelativePath("databases/usage_control.db"))
        assertFalse(IconStore.isSafeRelativePath("icons/../databases/x.png"))
        assertFalse(IconStore.isSafeRelativePath("/icons/com.a/$hash.png"))
        assertFalse(IconStore.isSafeRelativePath("icons/com.a/$hash.png.tmp"))
        assertTrue(IconStore.isSafeRelativePath("icons/com.a/$hash.png"))
    }

    @Test
    fun scaledSize_keepsSmallIcons() {
        assertEquals(48 to 24, IconStore.scaledSize(48, 24, 192))
        assertEquals(192 to 192, IconStore.scaledSize(192, 192, 192))
    }

    @Test
    fun scaledSize_capsLargeIconsPreservingAspect() {
        assertEquals(192 to 96, IconStore.scaledSize(1024, 512, 192))
        assertEquals(96 to 192, IconStore.scaledSize(512, 1024, 192))
    }

    @Test
    fun scaledSize_nonPositiveFallsBackToMax() {
        assertEquals(192 to 192, IconStore.scaledSize(-1, -1, 192))
        assertEquals(192 to 192, IconStore.scaledSize(0, 100, 192))
    }

    @Test
    fun scaledSize_extremeAspectNeverZero() {
        assertEquals(192 to 1, IconStore.scaledSize(10000, 1, 192))
    }
}
