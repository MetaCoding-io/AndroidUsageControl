package com.usagecontrol.feature.notifications.apps

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.AppNameResolver
import com.usagecontrol.core.database.dao.AppAlertCountRow
import com.usagecontrol.core.database.dao.NotificationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class AppsUiState(
    val loading: Boolean = true,
    val rows: List<AppRowUi> = emptyList(),
    val windowDays: Int = AppsViewModel.WINDOW_DAYS,
    val error: Boolean = false,
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Package label cache; PackageManager lookups are slow and labels rarely change. */
    private val labels = ConcurrentHashMap<String, String>()

    /** Local date, re-checked every minute so the 7-day window rolls at midnight. */
    private val today = flow {
        while (true) {
            emit(LocalDate.now(zone))
            delay(CLOCK_TICK_MS)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<AppsUiState> = today.flatMapLatest { day ->
        val since = day.minusDays(WINDOW_DAYS - 1L).atStartOfDay(zone).toInstant().toEpochMilli()
        combine(
            notificationDao.appAlertCountsFlow(since),
            notificationDao.appDailyCountsFlow(since),
        ) { counts, dayCounts ->
            AppsUiState(
                loading = false,
                rows = AppsLogic.buildRows(counts, dayCounts, WINDOW_DAYS, day, ::labelOf),
            )
        }
    }.flowOn(Dispatchers.IO)
        .catch { e ->
            Log.e(TAG, "apps aggregate query failed", e)
            emit(AppsUiState(loading = false, error = true))
        }
        .onEach { s -> Log.d(TAG, "state: apps=${s.rows.size} error=${s.error}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppsUiState())

    private fun labelOf(row: AppAlertCountRow): String = labels.getOrPut(row.packageName) {
        resolveLabel(context, row.packageName, row.appName)
    }

    companion object {
        const val WINDOW_DAYS = 7
        private const val TAG = "AppsViewModel"
        private const val CLOCK_TICK_MS = 60_000L
    }
}

/**
 * Installed label via [AppNameResolver]; when the app is gone (resolver falls back to the last
 * package segment) prefer the name captured with the notification.
 */
internal fun resolveLabel(context: Context, packageName: String, storedName: String?): String {
    val resolved = AppNameResolver.getAppName(context, packageName)
    val fallback = packageName.substringAfterLast(".")
    return if (resolved == fallback && !storedName.isNullOrBlank()) storedName else resolved
}
