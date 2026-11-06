package com.usagecontrol.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.usagecontrol.core.common.ScreenerLink
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.RouteKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the one-time Screener prompt for a new sender (R014): lane action buttons that
 * route the sender via [ScreenerActionReceiver], and a content intent opening the
 * Screener sheet. Never touches the sender's original notification.
 *
 * Dedupe is in-memory (fast path on the capture thread) backed by a SharedPreferences
 * entry per sender so a process restart does not re-prompt.
 */
@Singleton
class ScreenerNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prompted: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    @Volatile
    private var channelCreated = false

    fun alreadyPrompted(target: ScreenerTarget): Boolean {
        val key = target.dedupeKey
        if (key in prompted) return true
        if (prefs.contains(key)) {
            prompted += key
            return true
        }
        return false
    }

    /**
     * Posts the prompt and marks [target] prompted. Returns false (and leaves the sender
     * unprompted so a later post retries) when Doorman cannot post notifications.
     */
    fun post(
        target: ScreenerTarget,
        appName: String,
        title: String?,
        text: String?,
        lanes: List<Lane>,
        suggestedLaneId: Long,
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "screener skipped ${target.dedupeKey}: notifications disabled for Doorman")
            return false
        }
        ensureChannel()

        val notificationId = ScreenerMapping.notificationIdFor(target)
        val actionLanes = ScreenerMapping.actionLanes(lanes, suggestedLaneId)
        val suggested = lanes.firstOrNull { it.id == suggestedLaneId }
        val sender = if (target.kind == RouteKind.PERSON) "${target.key} · $appName" else appName
        val preview = listOfNotNull(title?.takeIf { it.isNotBlank() }, text?.takeIf { it.isNotBlank() })
            .joinToString(": ")

        val builder = NotificationCompat.Builder(context, ScreenerMapping.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setContentTitle("New sender: $sender")
            .setContentText(preview.ifEmpty { "Choose a lane" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview.ifEmpty { "Choose a lane" }))
            .setSubText(suggested?.let { "Filed in ${it.name} for now" })
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setContentIntent(sheetIntent(target, notificationId))

        actionLanes.forEachIndexed { index, lane ->
            builder.addAction(0, lane.name, routeIntent(target, lane.id, notificationId, index))
        }

        return try {
            manager.notify(notificationId, builder.build())
            markPrompted(target)
            Log.i(TAG, "screener posted ${target.dedupeKey} id=$notificationId lanes=${actionLanes.map { it.id }}")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "screener post denied for ${target.dedupeKey}", e)
            false
        }
    }

    fun cancel(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun markPrompted(target: ScreenerTarget) {
        prompted += target.dedupeKey
        prefs.edit().putLong(target.dedupeKey, System.currentTimeMillis()).apply()
    }

    private fun ensureChannel() {
        if (channelCreated) return
        val channel = NotificationChannel(
            ScreenerMapping.CHANNEL_ID,
            "Screener",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Asks once where notifications from a new sender should go"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        channelCreated = true
    }

    private fun sheetIntent(target: ScreenerTarget, notificationId: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, ScreenerLink.uri(target.packageName))
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun routeIntent(target: ScreenerTarget, laneId: Long, notificationId: Int, index: Int): PendingIntent {
        val intent = ScreenerActionReceiver.routeIntent(context, target, laneId, notificationId)
        // Distinct request codes per (prompt, action) so extras are not collapsed.
        val requestCode = notificationId * ScreenerMapping.MAX_ACTIONS + index
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        private const val TAG = "Lanes"
        private const val PREFS_NAME = "screener_prompted"
        private const val GROUP_KEY = "doorman.screener"
    }
}
