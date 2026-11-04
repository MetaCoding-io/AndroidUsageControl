package com.usagecontrol.feature.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.NotificationActions
import com.usagecontrol.core.common.NotificationActions.OpenResult
import com.usagecontrol.core.common.PermissionChecker
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.ContextSnapshot
import com.usagecontrol.core.model.NotificationAction
import com.usagecontrol.core.model.NotificationRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

/** One-shot messages for the Now snackbar. */
sealed interface NowEvent {
    /** A row was archived; the snackbar offers Undo until the commit delay elapses. */
    data class Archived(val recordId: Long, val title: String) : NowEvent

    /** A plain message; [isError] marks failures such as an unbound listener. */
    data class Message(val text: String, val isError: Boolean = false) : NowEvent
}

@HiltViewModel
class NowViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
    private val contextSnapshotDao: ContextSnapshotDao,
    private val permissionChecker: PermissionChecker,
    private val notificationActions: NotificationActions,
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

    private val _events = Channel<NowEvent>(Channel.BUFFERED)
    val events: Flow<NowEvent> = _events.receiveAsFlow()

    /** Archives waiting out the undo window, by record id; each commits to the shade when it fires. */
    private val pendingArchives = mutableMapOf<Long, PendingArchive>()

    private class PendingArchive(val key: String?, val job: Job)

    /**
     * Hides [record] from Now right away and clears it from the shade after [ARCHIVE_UNDO_MS]
     * unless [undoArchive] runs first.
     */
    fun archive(record: NotificationRecord) {
        val id = record.id
        pendingArchives.remove(id)?.job?.cancel()
        // Only rows still in the shade have anything to cancel there.
        val key = record.key?.takeIf { record.removedAt == null }
        viewModelScope.launch {
            notificationDao.markUserAction(id, NotificationAction.ARCHIVED.name, System.currentTimeMillis())
            Log.i(TAG, "archive id=$id key=$key: hidden, commit in ${ARCHIVE_UNDO_MS}ms")
            _events.send(NowEvent.Archived(id, record.title?.takeIf { it.isNotBlank() } ?: record.appName))
        }
        val job = viewModelScope.launch {
            delay(ARCHIVE_UNDO_MS)
            pendingArchives.remove(id)
            if (key != null) commitArchive(id, key)
        }
        pendingArchives[id] = PendingArchive(key, job)
    }

    /** Cancels a pending archive and restores the row to Now. */
    fun undoArchive(recordId: Long) {
        val pending = pendingArchives.remove(recordId)
        pending?.job?.cancel()
        viewModelScope.launch {
            notificationDao.clearUserAction(recordId)
            Log.i(TAG, "archive id=$recordId: undone (pending=${pending != null})")
        }
    }

    private suspend fun commitArchive(id: Long, key: String) {
        val result = withContext(Dispatchers.IO) { notificationActions.archive(key) }
        when (result) {
            NotificationActions.Result.DONE -> Unit
            NotificationActions.Result.LISTENER_UNAVAILABLE -> _events.send(
                NowEvent.Message("Notification access is off, so it's still in the shade", isError = true),
            )
            NotificationActions.Result.FAILED -> _events.send(
                NowEvent.Message("Couldn't clear it from the shade", isError = true),
            )
        }
        Log.d(TAG, "archive id=$id key=$key: commit $result")
    }

    /** Snoozes [record] until the next [hour]:[minute] local time. */
    fun snooze(record: NotificationRecord, hour: Int, minute: Int) {
        viewModelScope.launch {
            val key = record.key
            if (key == null || record.removedAt != null) {
                Log.w(TAG, "snooze id=${record.id}: not in the shade (key=$key removedAt=${record.removedAt})")
                _events.send(NowEvent.Message("Only notifications still in the shade can be snoozed"))
                return@launch
            }
            val duration = snoozeDurationUntil(hour, minute, System.currentTimeMillis(), zone)
            val result = withContext(Dispatchers.IO) { notificationActions.snooze(key, duration) }
            Log.i(TAG, "snooze id=${record.id} key=$key durationMs=$duration: $result")
            when (result) {
                NotificationActions.Result.DONE -> {
                    notificationDao.markUserAction(record.id, NotificationAction.SNOOZED.name, System.currentTimeMillis())
                    _events.send(NowEvent.Message("Snoozed until ${LocalTime.of(hour, minute).format(CLOCK_FORMAT)}"))
                }
                NotificationActions.Result.LISTENER_UNAVAILABLE -> _events.send(
                    NowEvent.Message("Notification access is off, so snooze isn't available", isError = true),
                )
                NotificationActions.Result.FAILED -> _events.send(
                    NowEvent.Message("Couldn't snooze this notification", isError = true),
                )
            }
        }
    }

    /** Opens the original notification target, or reports that it is gone. */
    fun open(record: NotificationRecord) {
        viewModelScope.launch {
            val result = notificationActions.open(record.key, record.packageName)
            Log.i(TAG, "open id=${record.id} key=${record.key}: $result")
            if (result != OpenResult.OPENED) {
                _events.send(NowEvent.Message("Original no longer available"))
            }
        }
    }

    override fun onCleared() {
        // The scope is already cancelled; commit archives whose undo window was cut short.
        val pending = pendingArchives.toMap()
        pendingArchives.clear()
        pending.forEach { (id, p) ->
            p.job.cancel()
            p.key?.let { key ->
                val result = notificationActions.archive(key)
                Log.d(TAG, "archive id=$id key=$key: commit on clear $result")
            }
        }
        super.onCleared()
    }

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
        const val ARCHIVE_UNDO_MS = 10_000L
        val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
