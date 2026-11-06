package com.usagecontrol.feature.notifications.screener

import com.usagecontrol.core.database.dao.UnroutedRow
import com.usagecontrol.core.model.RouteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenerLogicTest {

    private fun row(
        id: Long,
        pkg: String,
        ts: Long,
        title: String? = "t$id",
        conversation: Boolean = false,
        shortcutId: String? = null,
        laneId: Long? = 3,
        appName: String = pkg.substringAfterLast('.'),
    ) = UnroutedRow(id, pkg, appName, title, "body$id", ts, conversation, shortcutId, null, laneId)

    @Test
    fun `non-conversation rows group per app with newest sample and count`() {
        val senders = ScreenerLogic.unscreenedSenders(
            listOf(row(1, "com.shop", 100), row(2, "com.shop", 300), row(3, "com.news", 200)),
        )
        assertEquals(listOf("com.shop", "com.news"), senders.map { it.packageName })
        val shop = senders.first()
        assertEquals(RouteKind.APP, shop.kind)
        assertEquals("", shop.key)
        assertEquals(2, shop.count)
        assertEquals("t2", shop.sampleTitle)
        assertEquals(300L, shop.lastTimestamp)
    }

    @Test
    fun `conversations are screened per person using shortcut id over title`() {
        val senders = ScreenerLogic.unscreenedSenders(
            listOf(
                row(1, "com.chat", 100, title = "Alice", conversation = true),
                row(2, "com.chat", 200, title = "Bob", conversation = true, shortcutId = "bob-1"),
                row(3, "com.chat", 300, title = "Alice", conversation = true),
                row(4, "com.chat", 400, title = "Backup done"),
            ),
        )
        assertEquals(listOf("APP|com.chat|", "PERSON|com.chat|Alice", "PERSON|com.chat|bob-1"), senders.map { it.id })
        assertEquals(2, senders.first { it.key == "Alice" }.count)
        assertEquals("Alice · chat", senders.first { it.key == "Alice" }.displayName)
    }

    @Test
    fun `conversation without any person key falls back to app sender`() {
        val (kind, key) = ScreenerLogic.senderOf(row(1, "com.chat", 1, title = null, conversation = true, shortcutId = ""))
        assertEquals(RouteKind.APP, kind)
        assertEquals("", key)
    }

    @Test
    fun `focus package moves to top keeping relative order`() {
        val rows = listOf(row(1, "com.a", 300), row(2, "com.b", 200), row(3, "com.c", 100))
        assertEquals(
            listOf("com.c", "com.a", "com.b"),
            ScreenerLogic.unscreenedSenders(rows, focusPackage = "com.c").map { it.packageName },
        )
        // Unknown or blank focus leaves the order alone.
        assertEquals(
            listOf("com.a", "com.b", "com.c"),
            ScreenerLogic.unscreenedSenders(rows, focusPackage = "com.missing").map { it.packageName },
        )
        assertEquals(listOf("com.a", "com.b", "com.c"), ScreenerLogic.unscreenedSenders(rows, "").map { it.packageName })
    }

    @Test
    fun `empty input and blank app names`() {
        assertTrue(ScreenerLogic.unscreenedSenders(emptyList()).isEmpty())
        val s = ScreenerLogic.unscreenedSenders(listOf(row(1, "com.x", 1, appName = " "))).single()
        assertEquals("com.x", s.appName)
    }

    @Test
    fun `pre-v5 rows keep a null current lane`() {
        assertEquals(null, ScreenerLogic.unscreenedSenders(listOf(row(1, "com.x", 1, laneId = null))).single().currentLaneId)
    }
}
