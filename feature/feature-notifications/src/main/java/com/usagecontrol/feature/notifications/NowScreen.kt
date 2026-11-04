package com.usagecontrol.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    NowContent(state = state, onQueryChange = viewModel::setQuery)
}

@Composable
private fun NowContent(state: NowUiState, onQueryChange: (String) -> Unit) {
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
            section.items.forEach { item ->
                when (item) {
                    is SingleItem -> item(key = item.id) {
                        NotificationItem(item.record, Modifier.padding(horizontal = 12.dp))
                    }
                    is BundleItem -> bundleItems(
                        bundle = item,
                        expanded = item.id in expandedBundles,
                        onToggle = {
                            expandedBundles = if (item.id in expandedBundles) {
                                expandedBundles - item.id
                            } else {
                                expandedBundles + item.id
                            }
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
                    NotificationItem(record, Modifier.padding(horizontal = 12.dp), unread = false)
                }
            }
        }
    }
}

private fun LazyListScope.bundleItems(bundle: BundleItem, expanded: Boolean, onToggle: () -> Unit) {
    item(key = bundle.id) {
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
        items(bundle.rows, key = { "${bundle.id}:${it.id}" }) { record ->
            NotificationItem(record, Modifier.padding(start = 28.dp, end = 12.dp))
        }
    }
}

@Composable
private fun NotificationItem(record: NotificationRecord, modifier: Modifier = Modifier, unread: Boolean = true) {
    NotificationRow(
        lane = record.lane,
        appName = record.appName,
        title = record.title?.takeIf { it.isNotBlank() } ?: record.appName,
        text = record.text,
        time = TimeUtils.formatTime(record.timestamp),
        unread = unread,
        icon = { NotificationIcon(record) },
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
