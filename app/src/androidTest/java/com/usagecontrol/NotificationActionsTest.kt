package com.usagecontrol

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.usagecontrol.core.common.NotificationActions.OpenResult
import com.usagecontrol.core.common.NotificationActions.Result
import com.usagecontrol.service.notification.NotificationActionsImpl
import com.usagecontrol.service.notification.PendingIntentCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Drives snooze and tap-to-open through the real [NotificationActionsImpl] against the
 * system: snooze goes through the bound listener, open sends a real PendingIntent.
 */
@RunWith(AndroidJUnit4::class)
class NotificationActionsTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val cache = PendingIntentCache()
    private val actions = NotificationActionsImpl(context, cache)

    @Before
    fun setUp() {
        val ui = instrumentation.uiAutomation
        ui.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        ui.executeShellCommand("cmd notification allow_listener $LISTENER").close()
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Test", NotificationManager.IMPORTANCE_DEFAULT))
        waitUntil("listener connected") { actions.isListenerConnected }
    }

    @After
    fun tearDown() {
        nm.cancel(NOTIFICATION_ID)
    }

    @Test
    fun snooze_removesNotificationFromShade() {
        nm.notify(NOTIFICATION_ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SnoozeTest")
            .build())
        val key = waitFor("posted") { nm.activeNotifications.firstOrNull { it.id == NOTIFICATION_ID }?.key }

        assertEquals(Result.DONE, actions.snooze(key, 60_000))

        // The poster's own activeNotifications still lists snoozed entries on API 36, so ask
        // the system's snooze list instead.
        try {
            waitUntil("system snoozed $key") { shell("dumpsys notification").contains("key: $key until:") }
        } finally {
            shell("cmd notification unsnooze $key")
        }
    }

    @Test
    fun snooze_nonPositiveDuration_fails() {
        assertEquals(Result.FAILED, actions.snooze("0|com.usagecontrol|1|null|0", 0))
    }

    @Test
    fun open_sendsCachedContentIntent() {
        val action = "com.usagecontrol.test.OPEN_${SystemClock.uptimeMillis()}"
        val received = CountDownLatch(1)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = received.countDown()
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
        try {
            val pi = PendingIntent.getBroadcast(
                context, 0, Intent(action).setPackage(context.packageName), PendingIntent.FLAG_IMMUTABLE,
            )
            cache.put("k-open", pi)

            assertEquals(OpenResult.OPENED, actions.open("k-open", context.packageName))
            assertTrue("content intent was not delivered", received.await(5, TimeUnit.SECONDS))
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    @Test
    fun open_cancelledIntent_fallsBackToLaunchAndEvicts() {
        val pi = PendingIntent.getBroadcast(
            context, 1, Intent("com.usagecontrol.test.GONE").setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        pi.cancel()
        cache.put("k-gone", pi)

        assertEquals(OpenResult.FALLBACK_LAUNCHED, actions.open("k-gone", context.packageName))
        assertNull("cancelled intent must be evicted", cache.get("k-gone"))
    }

    @Test
    fun open_noIntentAndNoLauncher_isUnavailable() {
        assertEquals(OpenResult.UNAVAILABLE, actions.open(null, "com.example.does.not.exist"))
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }

    private fun waitUntil(what: String, condition: () -> Boolean) {
        waitFor(what) { condition().takeIf { it } }
    }

    private fun <T : Any> waitFor(what: String, probe: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            probe()?.let { return it }
            SystemClock.sleep(100)
        }
        return probe() ?: throw AssertionError("timed out waiting for $what")
    }

    private companion object {
        const val CHANNEL = "actions_test"
        const val NOTIFICATION_ID = 4242
        const val LISTENER = "com.usagecontrol/com.usagecontrol.service.notification.NotificationCaptureService"
    }
}
