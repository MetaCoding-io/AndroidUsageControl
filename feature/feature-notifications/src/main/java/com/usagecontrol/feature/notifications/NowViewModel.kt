package com.usagecontrol.feature.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.PermissionChecker
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.ContextSnapshot
import com.usagecontrol.core.model.NotificationRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** One time section of the Now stream with its (possibly bundled) items. */
data class NowSectionUi(val section: NowSection, val items: List<StreamItem>)

/** Current context for the strip; [locationLabel] null when nothing has been sampled yet. */
data class NowContextUi(
    val locationLabel: String?,
    val event: String?,
    val liveCount: Int,
    val listenerGranted: Boolean,
)

data class NowUiState(
    val loading: Boolean = true,
    val query: String = "",
    val heroCount: Int = 0,
    val sections: List<NowSectionUi> = emptyList(),
    val recentlyDismissed: List<NotificationRecord> = emptyList(),
    val context: NowContextUi = NowContextUi(null, null, 0, listenerGranted = true),
    val now: Long = System.currentTimeMillis(),
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

@HiltViewModel
class NowViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
    private val contextSnapshotDao: ContextSnapshotDao,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val query = MutableStateFlow("")
    private val listenerGranted = MutableStateFlow(permissionChecker.isNotificationAccessGranted())

    /** Wall clock, re-emitted every minute so sections, the 1 h dismissed window and the day roll forward. */
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(CLOCK_TICK_MS)
        }
    }

    private val dayStart = clock.map { startOfDay(it) }.distinctUntilChanged()

    // Room queries restart only when the local day changes, not every tick.
    private val stream = dayStart.flatMapLatest { start ->
        notificationDao.nowStreamFlow(since = start - DAY_MS)
    }.map { list -> list.map { it.toModel() } }
        .catch { e ->
            Log.e(TAG, "nowStreamFlow failed", e)
            emit(emptyList())
        }

    // Queried from the start of the day; the 1 h cutoff is applied in memory against the clock.
    private val dismissed = dayStart.flatMapLatest { start ->
        notificationDao.recentlyDismissedFlow(since = start)
    }.map { list -> list.map { it.toModel() } }
        .catch { e ->
            Log.e(TAG, "recentlyDismissedFlow failed", e)
            emit(emptyList())
        }

    private val heroCount = dayStart.flatMapLatest { start ->
        notificationDao.todayInterruptionCount(start)
    }.catch { e ->
        Log.e(TAG, "todayInterruptionCount failed", e)
        emit(0)
    }

    private val latestContext = clock.map {
        try {
            contextSnapshotDao.getLatest()?.toModel()
        } catch (e: Exception) {
            Log.w(TAG, "context snapshot read failed", e)
            null
        }
    }.distinctUntilChanged()

    private val data = combine(stream, dismissed, heroCount, latestContext, ::NowData)

    val uiState: StateFlow<NowUiState> = combine(data, clock, query, listenerGranted) { d, now, q, granted ->
        buildState(d, now, q, granted)
    }.onEach { s ->
        Log.d(TAG, "state: rows=${s.sections.sumOf { it.items.size }} dismissed=${s.recentlyDismissed.size} hero=${s.heroCount}")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NowUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun recheckPermission() {
        listenerGranted.value = permissionChecker.isNotificationAccessGranted()
    }

    private fun buildState(d: NowData, now: Long, q: String, granted: Boolean): NowUiState {
        val visible = filter(d.stream, q)
        val sections = sectionize(visible, now, zone).map { (section, rows) ->
            NowSectionUi(section, bundle(rows))
        }
        val dismissedCutoff = now - DISMISSED_WINDOW_MS
        val recent = filter(d.dismissed.filter { (it.removedAt ?: 0L) >= dismissedCutoff }, q)
        val live = d.stream.count { it.key != null && it.removedAt == null }
        return NowUiState(
            loading = false,
            query = q,
            heroCount = d.heroCount,
            sections = sections,
            recentlyDismissed = recent,
            context = NowContextUi(
                locationLabel = d.context?.locationType?.name,
                event = d.context?.calendarEvent,
                liveCount = live,
                listenerGranted = granted,
            ),
            now = now,
        )
    }

    private fun startOfDay(now: Long): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    private data class NowData(
        val stream: List<NotificationRecord>,
        val dismissed: List<NotificationRecord>,
        val heroCount: Int,
        val context: ContextSnapshot?,
    )

    private companion object {
        const val TAG = "NowViewModel"
        const val CLOCK_TICK_MS = 60_000L
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val DISMISSED_WINDOW_MS = 60 * 60 * 1000L
    }
}
