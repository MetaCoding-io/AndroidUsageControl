package com.usagecontrol.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.model.NotificationRecord
import com.usagecontrol.core.ui.component.BundleRow
import com.usagecontrol.core.ui.component.ContextStrip
import com.usagecontrol.core.ui.component.EmptyState
import com.usagecontrol.core.ui.component.HeroCount
import com.usagecontrol.core.ui.component.NotificationRow
import com.usagecontrol.core.ui.component.SectionHeader
import kotlinx.coroutines.launch
import java.util.Calendar

/** The daily Now stream: hero count, context strip, search, time sections and recently dismissed. */
@Composable
fun NowScreen(viewModel: NowViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.recheckPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NowEvent.Archived -> launch {
                    // Long is 10 s, matching the archive commit delay.
                    val result = snackbarHostState.showSnackbar(
                        message = "Archived \"${event.title}\"",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoArchive(event.recordId)
                }
                is NowEvent.Message -> launch {
                    snackbarHostState.showSnackbar(
                        message = event.text,
                        duration = if (event.isError) SnackbarDuration.Long else SnackbarDuration.Short,
                        withDismissAction = event.isError,
                    )
                }
            }
        }
    }

    var snoozeTarget by remember { mutableStateOf<NotificationRecord?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        NowContent(
            state = state,
            onQueryChange = viewModel::setQuery,
            rowActions = RowActions(
                onOpen = viewModel::open,
                onArchive = viewModel::archive,
                onSnoozeRequest = { snoozeTarget = it },
            ),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).testTag("now_snackbar"),
        )
    }

    snoozeTarget?.let { record ->
        SnoozeTimeDialog(
            onConfirm = { hour, minute ->
                snoozeTarget = null
                viewModel.snooze(record, hour, minute)
            },
            onDismiss = { snoozeTarget = null },
        )
    }
}

/** Callbacks shared by every swipeable stream row. */
internal class RowActions(
    val onOpen: (NotificationRecord) -> Unit,
    val onArchive: (NotificationRecord) -> Unit,
    val onSnoozeRequest: (NotificationRecord) -> Unit,
)

@Composable
private fun NowContent(state: NowUiState, onQueryChange: (String) -> Unit, rowActions: RowActions) {
    var expandedBundles by remember { mutableStateOf(emptySet<String>()) }
    var dismissedExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("now_list"),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "hero") {
            HeroCount(
                value = state.heroCount.toString(),
                label = "interruptions today",
                modifier = Modifier.testTag("now_hero"),
            )
        }
        item(key = "context") {
            NowContextStrip(state.context)
        }
        item(key = "search") {
            SearchField(query = state.query, onQueryChange = onQueryChange)
        }

        if (state.isEmpty && !state.loading) {
            item(key = "empty") {
                EmptyState(
                    message = if (state.query.isBlank()) {
                        "All quiet. New notifications will appear here as they arrive."
                    } else {
                        "Nothing in Now matches \"${state.query.trim()}\"."
                    },
                    modifier = Modifier.padding(vertical = 32.dp).testTag("now_empty"),
                )
            }
        }

        state.sections.forEach { section ->
            item(key = "section:${section.section.name}") {
                SectionHeader(
                    title = sectionTitle(section.section),
                    meta = section.items.sumOf { if (it is BundleItem) it.count else 1 }.toString(),
                )
            }
            // One app can bundle in several sections, so list keys are scoped by section.
            val scope = section.section.name
            section.items.forEach { item ->
                when (item) {
                    is SingleItem -> item(key = "$scope:${item.id}") {
                        SwipeableNotificationItem(item.record, rowActions, Modifier.padding(horizontal = 12.dp))
                    }
                    is BundleItem -> bundleItems(
                        bundle = item,
                        keyScope = scope,
                        rowActions = rowActions,
                        expanded = "$scope:${item.id}" in expandedBundles,
                        onToggle = {
                            val id = "$scope:${item.id}"
                            expandedBundles = if (id in expandedBundles) expandedBundles - id else expandedBundles + id
                        },
                    )
                }
            }
        }

        if (state.recentlyDismissed.isNotEmpty()) {
            item(key = "dismissed_header") {
                RecentlyDismissedHeader(
                    count = state.recentlyDismissed.size,
                    expanded = dismissedExpanded,
                    onToggle = { dismissedExpanded = !dismissedExpanded },
                )
            }
            if (dismissedExpanded) {
                items(state.recentlyDismissed, key = { "d:${it.id}" }) { record ->
                    NotificationItem(
                        record,
                        Modifier.padding(horizontal = 12.dp),
                        unread = false,
                        onClick = { rowActions.onOpen(record) },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.bundleItems(
    bundle: BundleItem,
    keyScope: String,
    rowActions: RowActions,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    item(key = "$keyScope:${bundle.id}") {
        val newest = bundle.rows.first()
        BundleRow(
            lane = bundle.lane,
            title = bundle.appName,
            count = bundle.count,
            preview = listOfNotNull(newest.title, newest.text).joinToString(" · ").ifEmpty { null },
            time = TimeUtils.formatTime(bundle.timestamp),
            onClick = onToggle,
            modifier = Modifier.padding(horizontal = 12.dp).testTag("now_bundle"),
        )
    }
    if (expanded) {
        items(bundle.rows, key = { "$keyScope:${bundle.id}:${it.id}" }) { record ->
            SwipeableNotificationItem(record, rowActions, Modifier.padding(start = 28.dp, end = 12.dp))
        }
    }
}

/**
 * A stream row with swipe actions: start-to-end archives (with undo), end-to-start asks for a
 * snooze time and springs back; tap opens the original.
 */
@Composable
internal fun SwipeableNotificationItem(
    record: NotificationRecord,
    actions: RowActions,
    modifier: Modifier = Modifier,
    swipeState: SwipeToDismissBoxState = rememberSwipeToDismissBoxState(),
) {
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier.testTag("now_swipe"),
        backgroundContent = { SwipeBackground(swipeState.dismissDirection) },
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    actions.onArchive(record)
                    // The swipe state is saveable, so a row restored by Undo would come back
                    // already dismissed and archive itself again; settle it now.
                    scope.launch { swipeState.snapTo(SwipeToDismissBoxValue.Settled) }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Snooze keeps the row until a time is chosen; spring back and ask.
                    scope.launch { swipeState.reset() }
                    actions.onSnoozeRequest(record)
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
    ) {
        NotificationItem(record, onClick = { actions.onOpen(record) })
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val (icon, label, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(Icons.Default.Archive, "Archive", Alignment.CenterStart)
        SwipeToDismissBoxValue.EndToStart -> Triple(Icons.Default.Schedule, "Snooze", Alignment.CenterEnd)
        SwipeToDismissBoxValue.Settled -> return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** Picks the clock time to snooze until; the next occurrence of that time is used. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnoozeTimeDialog(onConfirm: (hour: Int, minute: Int) -> Unit, onDismiss: () -> Unit) {
    val initial = remember {
        Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }
    }
    val pickerState = rememberTimePickerState(
        initialHour = initial.get(Calendar.HOUR_OF_DAY),
        initialMinute = 0,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("now_snooze_dialog"),
        title = { Text("Snooze until") },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pickerState.hour, pickerState.minute) },
                modifier = Modifier.testTag("now_snooze_confirm"),
            ) { Text("Snooze") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun NotificationItem(
    record: NotificationRecord,
    modifier: Modifier = Modifier,
    unread: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    NotificationRow(
        lane = record.lane,
        appName = record.appName,
        title = record.title?.takeIf { it.isNotBlank() } ?: record.appName,
        text = record.text,
        time = TimeUtils.formatTime(record.timestamp),
        unread = unread,
        icon = { NotificationIcon(record) },
        onClick = onClick,
        modifier = modifier.testTag("now_row"),
    )
}

@Composable
private fun NowContextStrip(context: NowContextUi) {
    val text = when {
        !context.listenerGranted -> "Notification access is off"
        context.event != null -> context.event
        context.locationLabel != null && context.locationLabel != "UNKNOWN" ->
            context.locationLabel.lowercase().replaceFirstChar { it.uppercase() }
        else -> "No mode active"
    }
    ContextStrip(
        text = text,
        detail = "${context.liveCount} in shade",
        active = context.listenerGranted,
        modifier = Modifier.padding(horizontal = 12.dp).testTag("now_context"),
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag("now_search"),
        placeholder = { Text("Search notifications") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun RecentlyDismissedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.padding(horizontal = 4.dp).testTag("now_dismissed_toggle"),
        ) {
            Text(
                text = "Recently dismissed ($count)",
                style = MaterialTheme.typography.titleSmall,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
    }
}

private fun sectionTitle(section: NowSection): String = when (section) {
    NowSection.NOW -> "Now"
    NowSection.EARLIER_TODAY -> "Earlier today"
    NowSection.OLDER -> "Yesterday and older"
}
