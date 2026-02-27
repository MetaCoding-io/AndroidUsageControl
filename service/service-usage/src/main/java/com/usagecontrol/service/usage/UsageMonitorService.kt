package com.usagecontrol.service.usage

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.usagecontrol.core.common.AppNameResolver
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.entity.UsageEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class UsageMonitorService : Service() {

    @Inject lateinit var usageDao: UsageDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startPolling()
    }

    private fun startPolling() {
        scope.launch {
            while (true) {
                try {
                    pollUsageStats()
                } catch (e: Exception) {
                    // Log but don't crash
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val now = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            now,
        )

        val today = TimeUtils.todayDate()

        stats.filter { it.totalTimeInForeground > 0 }
            .forEach { stat ->
                val entity = UsageEntity(
                    packageName = stat.packageName,
                    appName = AppNameResolver.getAppName(applicationContext, stat.packageName),
                    date = today,
                    foregroundTimeMs = stat.totalTimeInForeground,
                    launchCount = 0, // Not directly available from UsageStats
                    firstUsedTimestamp = stat.firstTimeStamp,
                    lastUsedTimestamp = stat.lastTimeUsed,
                )
                usageDao.upsert(entity)
            }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Usage Monitoring",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Tracks app usage in the background"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Usage Control")
            .setContentText("Monitoring app usage")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "usage_monitor"
        private const val POLL_INTERVAL_MS = 60_000L // 1 minute
    }
}
