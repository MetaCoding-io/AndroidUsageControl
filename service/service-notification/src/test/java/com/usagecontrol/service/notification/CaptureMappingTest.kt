package com.usagecontrol.service.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureMappingTest {

    @Test
    fun isLive_plainNotification_isHistory() {
        assertFalse(CaptureMapping.isLive(flags = 0, template = null, hasMediaSession = false))
        assertFalse(
            CaptureMapping.isLive(
                flags = Notification.FLAG_AUTO_CANCEL or Notification.FLAG_GROUP_SUMMARY,
                template = "android.app.Notification\$BigTextStyle",
                hasMediaSession = false,
            ),
        )
    }

    @Test
    fun isLive_ongoingOrForegroundService() {
        assertTrue(CaptureMapping.isLive(Notification.FLAG_ONGOING_EVENT, null, false))
        assertTrue(CaptureMapping.isLive(Notification.FLAG_FOREGROUND_SERVICE, null, false))
    }

    @Test
    fun isLive_mediaTemplatesOrSession() {
        assertTrue(CaptureMapping.isLive(0, CaptureMapping.MEDIA_STYLE_TEMPLATE, false))
        assertTrue(CaptureMapping.isLive(0, CaptureMapping.DECORATED_MEDIA_STYLE_TEMPLATE, false))
        assertTrue(CaptureMapping.isLive(0, null, true))
    }

    @Test
    fun isLive_messagingTemplate_isHistory() {
        assertFalse(CaptureMapping.isLive(0, "android.app.Notification\$MessagingStyle", false))
    }

    @Test
    fun removalReasonName_mapsKnownReasons() {
        val expected = mapOf(
            NotificationListenerService.REASON_CLICK to "CLICK",
            NotificationListenerService.REASON_CANCEL to "CANCEL",
            NotificationListenerService.REASON_CANCEL_ALL to "CANCEL_ALL",
            NotificationListenerService.REASON_APP_CANCEL to "APP_CANCEL",
            NotificationListenerService.REASON_LISTENER_CANCEL to "LISTENER_CANCEL",
            NotificationListenerService.REASON_SNOOZED to "SNOOZED",
            NotificationListenerService.REASON_TIMEOUT to "TIMEOUT",
            NotificationListenerService.REASON_GROUP_SUMMARY_CANCELED to "GROUP_SUMMARY_CANCELED",
        )
        expected.forEach { (code, name) -> assertEquals(name, CaptureMapping.removalReasonName(code)) }
    }

    @Test
    fun removalReasonName_unknownCodes_areOther() {
        assertEquals("OTHER_0", CaptureMapping.removalReasonName(0))
        assertEquals("OTHER_-1", CaptureMapping.removalReasonName(-1))
        assertEquals("OTHER_9999", CaptureMapping.removalReasonName(9999))
    }

    @Test
    fun removalReasonName_namesAreUnique() {
        val names = (1..19).map(CaptureMapping::removalReasonName)
        assertEquals(names.size, names.toSet().size)
    }
}
