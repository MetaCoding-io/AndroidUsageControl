package com.usagecontrol.service.context

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.IBinder
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.entity.ContextSnapshotEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.RulePillar
import com.usagecontrol.core.rules.ActionExecutor
import com.usagecontrol.core.rules.EvaluationContext
import com.usagecontrol.core.rules.RuleEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ContextMonitorService : Service() {

    @Inject lateinit var contextSnapshotDao: ContextSnapshotDao
    @Inject lateinit var calendarReader: CalendarReader
    @Inject lateinit var bluetoothScanner: BluetoothScanner
    @Inject lateinit var soundPresetApplier: SoundPresetApplier
    @Inject lateinit var ruleEngine: RuleEngine
    @Inject lateinit var actionExecutor: ActionExecutor
    @Inject lateinit var actionHandlerRegistrar: ActionHandlerRegistrar

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        actionHandlerRegistrar.ensureRegistered()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startPolling()
    }

    private fun startPolling() {
        scope.launch {
            while (true) {
                try {
                    val snapshot = captureSnapshot()
                    soundPresetApplier.evaluateAndApply(snapshot.wifiSsid)
                    evaluateLockSoundRules(snapshot)
                } catch (_: Exception) {
                    // Log but don't crash
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun captureSnapshot(): ContextSnapshotEntity {
        val wifiSsid = getCurrentWifiSsid()
        val isCharging = isDeviceCharging()
        val calendarEvent = calendarReader.getCurrentEvent()
        val bluetoothDevice = bluetoothScanner.getConnectedDeviceName()

        val snapshot = ContextSnapshotEntity(
            timestamp = System.currentTimeMillis(),
            locationType = inferLocationType(wifiSsid),
            wifiSsid = wifiSsid,
            timeOfDay = TimeUtils.currentTimeOfDay(),
            dayOfWeek = TimeUtils.currentDayOfWeek(),
            isCharging = isCharging,
            calendarEvent = calendarEvent,
            bluetoothDevice = bluetoothDevice,
        )

        contextSnapshotDao.insert(snapshot)
        return snapshot
    }

    /**
     * Evaluate LOCK_SOUND rules against the current context snapshot.
     * Matching rules are executed via ActionExecutor which delegates to registered handlers.
     */
    private suspend fun evaluateLockSoundRules(snapshot: ContextSnapshotEntity) {
        val snapshotModel = snapshot.toModel()
        val evalContext = EvaluationContext(snapshot = snapshotModel)
        val matchingRules = ruleEngine.findMatchingRules(RulePillar.LOCK_SOUND, evalContext)
        if (matchingRules.isNotEmpty()) {
            actionExecutor.executeAll(matchingRules)
        }
    }

    @Suppress("DEPRECATION")
    private fun getCurrentWifiSsid(): String? {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        val info = wifiManager.connectionInfo ?: return null
        val ssid = info.ssid?.removeSurrounding("\"")
        return if (ssid == "<unknown ssid>" || ssid.isNullOrBlank()) null else ssid
    }

    private fun isDeviceCharging(): Boolean {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return false
        return batteryManager.isCharging
    }

    private fun inferLocationType(wifiSsid: String?): String {
        // Simple heuristic: could be extended with user-configured home/work SSIDs
        return when {
            wifiSsid == null -> "UNKNOWN"
            else -> "OTHER" // User can configure home/work SSIDs in settings later
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Context Monitoring",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Monitors device context (WiFi, charging, etc.)"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.context_service_notification_title))
            .setContentText("Monitoring device context")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "context_monitor"
        private const val POLL_INTERVAL_MS = 300_000L // 5 minutes
    }
}
