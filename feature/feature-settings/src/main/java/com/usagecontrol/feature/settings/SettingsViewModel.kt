package com.usagecontrol.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.UsageDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
    private val usageDao: UsageDao,
    private val contextSnapshotDao: ContextSnapshotDao,
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("usage_control_settings", Context.MODE_PRIVATE)

    private val _retentionDays = MutableStateFlow(prefs.getInt(KEY_RETENTION_DAYS, 30))
    val retentionDays: StateFlow<Int> = _retentionDays

    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount

    private val _cleanupResult = MutableStateFlow<String?>(null)
    val cleanupResult: StateFlow<String?> = _cleanupResult

    init {
        refreshCount()
    }

    fun setRetentionDays(days: Int) {
        _retentionDays.value = days
        prefs.edit().putInt(KEY_RETENTION_DAYS, days).apply()
    }

    fun runCleanup() {
        viewModelScope.launch {
            val cutoff = TimeUtils.daysAgo(_retentionDays.value)
            val cutoffDate = TimeUtils.formatDate(cutoff)

            val notifDeleted = notificationDao.deleteOlderThan(cutoff)
            val usageDeleted = usageDao.deleteOlderThan(cutoffDate)
            val contextDeleted = contextSnapshotDao.deleteOlderThan(cutoff)

            _cleanupResult.value = "Cleaned up: $notifDeleted notifications, $usageDeleted usage records, $contextDeleted context snapshots"
            refreshCount()
        }
    }

    fun clearCleanupResult() {
        _cleanupResult.value = null
    }

    private fun refreshCount() {
        viewModelScope.launch {
            _notificationCount.value = notificationDao.getCount()
        }
    }

    companion object {
        private const val KEY_RETENTION_DAYS = "retention_days"
    }
}
