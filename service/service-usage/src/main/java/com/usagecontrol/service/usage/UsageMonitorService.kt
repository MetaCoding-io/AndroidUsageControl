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
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.entity.UsageEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.ActionType
import com.usagecontrol.core.model.RulePillar
import com.usagecontrol.core.rules.EvaluationContext
import com.usagecontrol.core.rules.RuleEngine
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
    @Inject lateinit var ruleEngine: RuleEngine
    @Inject lateinit var contextSnapshotDao: ContextSnapshotDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val blockedThisCycle = mutableSetOf<String>()
    private var lastResetDate: String = ""

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
                    // Reset block tracking at midnight
                    val today = TimeUtils.todayDate()
                    if (today != lastResetDate) {
                        blockedThisCycle.clear()
                        lastResetDate = today
                    }
                    pollUsageStats()
                    evaluateScreenTimeRules()
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
                    launchCount = 0,
                    firstUsedTimestamp = stat.firstTimeStamp,
                    lastUsedTimestamp = stat.lastTimeUsed,
                )
                usageDao.upsert(entity)
            }
    }

    private suspend fun evaluateScreenTimeRules() {
        val today = TimeUtils.todayDate()
        val usageRecords = usageDao.getByDate(today)
        val snapshot = contextSnapshotDao.getLatest()?.toModel()

        for (record in usageRecords) {
            val evalContext = EvaluationContext(
                packageName = record.packageName,
                snapshot = snapshot,
            )

            val matchingRules = ruleEngine.findMatchingRules(RulePillar.SCREEN_TIME, evalContext)

            for (rule in matchingRules) {
                val usedMinutes = (record.foregroundTimeMs / 60_000).toInt()

                val limitMinutes = rule.actions.firstNotNullOfOrNull {
                    it.params["dailyLimitMinutes"]?.toIntOrNull()
                } ?: continue

                if (usedMinutes < limitMinutes) continue

                val blockType = when {
                    rule.actions.any { it.type == ActionType.HARD_BLOCK } -> "hard"
                    rule.actions.any { it.type == ActionType.SOFT_BLOCK } -> "soft"
                    rule.actions.any { it.type == ActionType.ADVISE } -> {
                        val advKey = "advise_${record.packageName}"
                        if (advKey !in blockedThisCycle) {
                            blockedThisCycle.add(advKey)
                            showAdvisoryNotification(
                                AppNameResolver.getAppName(applicationContext, record.packageName),
                                usedMinutes,
                                limitMinutes,
                            )
                        }
                        continue
                    }
                    else -> continue
                }

                val blockKey = "${record.packageName}_$blockType"
                if (blockKey in blockedThisCycle) continue
                blockedThisCycle.add(blockKey)

                launchBlockOverlay(
                    appName = AppNameResolver.getAppName(applicationContext, record.packageName),
                    packageName = record.packageName,
                    blockType = blockType,
                    dailyLimitMinutes = limitMinutes,
                    usedMinutes = usedMinutes,
                )
            }
        }
    }

    private fun launchBlockOverlay(
        appName: String,
        packageName: String,
        blockType: String,
        dailyLimitMinutes: Int,
        usedMinutes: Int,
    ) {
        val intent = Intent().apply {
            setClassName(applicationContext, "com.usagecontrol.feature.screentime.BlockOverlayActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("appName", appName)
            putExtra("packageName", packageName)
            putExtra("blockType", blockType)
            putExtra("dailyLimitMinutes", dailyLimitMinutes)
            putExtra("usedMinutes", usedMinutes)
        }
        startActivity(intent)
    }

    private fun showAdvisoryNotification(appName: String, usedMinutes: Int, limitMinutes: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time Alert")
            .setContentText("You've used $appName for ${usedMinutes}min (limit: ${limitMinutes}min)")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
        nm.notify(appName.hashCode(), notification)
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
