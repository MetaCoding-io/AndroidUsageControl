package com.usagecontrol.feature.notifications.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.model.Suggestion
import com.usagecontrol.core.ui.component.DonutChart
import com.usagecontrol.core.ui.component.LoadingScreen
import com.usagecontrol.core.ui.component.SectionHeader
import com.usagecontrol.core.ui.component.SimpleBarChart
import com.usagecontrol.feature.notifications.AppIcon

/** Per-app profile: interruption stats, channels, screen-time limit and pending suggestions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLimitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(state.label.ifBlank { state.packageName }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("app_profile_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
            if (state.loading) {
                LoadingScreen(modifier = Modifier.testTag("app_profile_loading"))
            } else {
                ProfileContent(
                    state = state,
                    onEditLimit = { showLimitDialog = true },
                    onClearLimit = viewModel::clearLimit,
                    onAccept = viewModel::acceptSuggestion,
                    onDismiss = viewModel::dismissSuggestion,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).testTag("app_profile_snackbar"),
        )
    }

    if (showLimitDialog) {
        LimitDialog(
            initialMinutes = state.limitMinutes,
            onDismiss = { showLimitDialog = false },
            onConfirm = { minutes ->
                viewModel.setLimit(minutes)
                showLimitDialog = false
            },
        )
    }
}

@Composable
private fun ProfileContent(
    state: AppProfileUiState,
    onEditLimit: () -> Unit,
    onClearLimit: () -> Unit,
    onAccept: (Suggestion) -> Unit,
    onDismiss: (Suggestion) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("app_profile"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "ring") { RingSection(state) }

        item(key = "hourly") {
            SectionHeader(title = "When it notifies", meta = "by hour")
            SimpleBarChart(
                values = state.hourly.map { it.toFloat() },
                labels = listOf("0", "", "", "", "", "", "6", "", "", "", "", "", "12", "", "", "", "", "", "18", "", "", "", "", ""),
                highlightIndex = state.hourly.indices.maxByOrNull { state.hourly[it] }
                    ?.takeIf { state.hourly[it] > 0 } ?: -1,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("app_profile_hourly"),
            )
        }

        item(key = "channels-header") {
            SectionHeader(title = "Channels", meta = state.channels.size.toString())
        }
        if (state.channels.isEmpty()) {
            item(key = "channels-empty") { MutedLine("No channels recorded") }
        }
        items(state.channels, key = { "channel:${it.first}" }) { (name, count) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("app_profile_channel"),
            ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                Text(count.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }

        item(key = "screen-time") {
            SectionHeader(title = "Screen time")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).testTag("app_profile_limit"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Today: ${TimeUtils.formatDuration(state.todayScreenTimeMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    state.limitMinutes?.let { "Daily limit: $it min" } ?: "No daily limit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEditLimit, modifier = Modifier.testTag("app_profile_set_limit")) {
                        Text(if (state.limitMinutes == null) "Set limit" else "Change limit")
                    }
                    if (state.limitMinutes != null) {
                        TextButton(onClick = onClearLimit, modifier = Modifier.testTag("app_profile_clear_limit")) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        item(key = "suggestions-header") {
            SectionHeader(title = "Suggestions", meta = state.suggestions.size.toString())
        }
        if (state.suggestions.isEmpty()) {
            item(key = "suggestions-empty") { MutedLine("Nothing to suggest for this app yet") }
        }
        items(state.suggestions, key = { "suggestion:${it.id}" }) { suggestion ->
            SuggestionCard(suggestion, onAccept = { onAccept(suggestion) }, onDismiss = { onDismiss(suggestion) })
        }

        if (state.error) {
            item(key = "error") { MutedLine("Couldn't load stats for this app") }
        }
    }
}

@Composable
private fun RingSection(state: AppProfileUiState) {
    val dismissColor = MaterialTheme.colorScheme.error
    val actedColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("app_profile_ring"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = state.packageName, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        DonutChart(
            segments = if (state.total == 0) {
                listOf(1f to emptyColor)
            } else {
                listOf(state.dismissed.toFloat() to dismissColor, state.acted.toFloat() to actedColor)
            },
            centerText = "${(AppStats.dismissRate(state.dismissed, state.total) * 100).toInt()}%\ndismissed",
        )
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${state.total} alerts in ${state.windowDays}d", style = MaterialTheme.typography.titleSmall)
            Text(
                AppsLogic.formatPerDay(AppStats.alertsPerDay(state.total, state.windowDays)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Dismissed ${state.dismissed} / acted ${state.acted}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Median time to act: ${state.medianTimeToActMs?.let(::formatGap) ?: "n/a"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("app_profile_median"),
            )
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: Suggestion, onAccept: () -> Unit, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("app_profile_suggestion")) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(suggestion.reason, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Not now") }
                TextButton(onClick = onAccept, modifier = Modifier.testTag("app_profile_accept")) { Text("Accept") }
            }
        }
    }
}

@Composable
private fun LimitDialog(initialMinutes: Int?, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(initialMinutes?.toString() ?: "60") }
    val minutes = text.toIntOrNull()?.takeIf { it in 1..MAX_LIMIT_MINUTES }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("app_profile_limit_dialog"),
        title = { Text("Daily limit") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter(Char::isDigit).take(4) },
                label = { Text("Minutes per day") },
                singleLine = true,
                isError = minutes == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onConfirm) }, enabled = minutes != null) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MutedLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

private const val MAX_LIMIT_MINUTES = 24 * 60

/** "45s", "12m", "3h 5m". */
internal fun formatGap(ms: Long): String {
    val seconds = ms / 1000
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
