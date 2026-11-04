package com.usagecontrol.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.service.notification.IconStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
    private val usageDao: UsageDao,
    private val contextSnapshotDao: ContextSnapshotDao,
    private val ruleDao: RuleDao,
    private val iconStore: IconStore,
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("usage_control_settings", Context.MODE_PRIVATE)

    private val _retentionDays = MutableStateFlow(prefs.getInt(KEY_RETENTION_DAYS, 30))
    val retentionDays: StateFlow<Int> = _retentionDays

    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount

    private val _ruleCount = MutableStateFlow(0)
    val ruleCount: StateFlow<Int> = _ruleCount

    private val _cleanupResult = MutableStateFlow<String?>(null)
    val cleanupResult: StateFlow<String?> = _cleanupResult

    private val _autoCleanupEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CLEANUP, true))
    val autoCleanupEnabled: StateFlow<Boolean> = _autoCleanupEnabled

    init {
        refreshCounts()
        if (_autoCleanupEnabled.value) {
            scheduleAutoCleanup()
        }
    }

    fun setRetentionDays(days: Int) {
        _retentionDays.value = days
        prefs.edit().putInt(KEY_RETENTION_DAYS, days).apply()
    }

    fun setAutoCleanupEnabled(enabled: Boolean) {
        _autoCleanupEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_CLEANUP, enabled).apply()
        if (enabled) {
            scheduleAutoCleanup()
        } else {
            cancelAutoCleanup()
        }
    }

    fun runCleanup() {
        viewModelScope.launch {
            val cutoff = TimeUtils.daysAgo(_retentionDays.value)
            val cutoffDate = TimeUtils.formatDate(cutoff)

            val notifDeleted = notificationDao.deleteOlderThan(cutoff)
            val usageDeleted = usageDao.deleteOlderThan(cutoffDate)
            val contextDeleted = contextSnapshotDao.deleteOlderThan(cutoff)
            val referencedIcons = notificationDao.getAllIconPaths().toSet()
            withContext(Dispatchers.IO) { iconStore.pruneUnreferenced(referencedIcons) }

            _cleanupResult.value = "Cleaned up: $notifDeleted notifications, $usageDeleted usage records, $contextDeleted snapshots"
            refreshCounts()
        }
    }

    fun clearCleanupResult() {
        _cleanupResult.value = null
    }

    fun exportRulesJson(callback: (String) -> Unit) {
        viewModelScope.launch {
            val rules = ruleDao.getAll()
            val sb = StringBuilder()
            sb.appendLine("[")
            rules.forEachIndexed { index, rule ->
                sb.append("  {")
                sb.append("\"name\": \"${rule.name}\", ")
                sb.append("\"pillar\": \"${rule.pillar}\", ")
                sb.append("\"enabled\": ${rule.enabled}, ")
                sb.append("\"priority\": ${rule.priority}, ")
                sb.append("\"conditions\": ${rule.conditionsJson}, ")
                sb.append("\"actions\": ${rule.actionsJson}")
                sb.append("}")
                if (index < rules.size - 1) sb.appendLine(",")
            }
            sb.appendLine()
            sb.appendLine("]")
            callback(sb.toString())
        }
    }

    private fun scheduleAutoCleanup() {
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun cancelAutoCleanup() {
        WorkManager.getInstance(context).cancelUniqueWork(CleanupWorker.WORK_NAME)
    }

    private fun refreshCounts() {
        viewModelScope.launch {
            _notificationCount.value = notificationDao.getCount()
            _ruleCount.value = ruleDao.getAll().size
        }
    }

    companion object {
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_AUTO_CLEANUP = "auto_cleanup_enabled"
    }
}
