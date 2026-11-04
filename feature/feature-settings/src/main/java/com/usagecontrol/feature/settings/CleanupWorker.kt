package com.usagecontrol.feature.settings

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.SuggestionDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.service.notification.IconStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that runs daily to clean up old data
 * based on the configured retention period.
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationDao: NotificationDao,
    private val usageDao: UsageDao,
    private val contextSnapshotDao: ContextSnapshotDao,
    private val suggestionDao: SuggestionDao,
    private val iconStore: IconStore,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("usage_control_settings", Context.MODE_PRIVATE)
        val retentionDays = prefs.getInt("retention_days", 30)
        val cutoff = TimeUtils.daysAgo(retentionDays)
        val cutoffDate = TimeUtils.formatDate(cutoff)

        notificationDao.deleteOlderThan(cutoff)
        usageDao.deleteOlderThan(cutoffDate)
        contextSnapshotDao.deleteOlderThan(cutoff)
        suggestionDao.deleteResolved(cutoff)
        iconStore.pruneUnreferenced(notificationDao.getAllIconPaths().toSet())

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "data_cleanup"
    }
}
