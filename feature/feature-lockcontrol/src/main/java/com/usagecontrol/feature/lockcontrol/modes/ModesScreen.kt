package com.usagecontrol.feature.lockcontrol.modes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.model.PresetTrigger
import com.usagecontrol.core.model.RingerMode
import com.usagecontrol.core.model.SoundPreset
import com.usagecontrol.core.ui.component.ContextStrip
import com.usagecontrol.core.ui.component.EmptyState
import com.usagecontrol.core.ui.component.LoadingScreen
import com.usagecontrol.core.ui.component.SectionHeader
import com.usagecontrol.core.ui.theme.DoormanMonoType

/** Modes tab: sound presets as cards whose toggle applies the preset to the device. */
@Composable
fun ModesScreen(viewModel: ModesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbar.collect { snackbarHostState.showSnackbar(it) }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        ModesContent(
            state = state,
            contentPadding = padding,
            onToggle = viewModel::onToggle,
        )
    }
}

@Composable
private fun ModesContent(
    state: ModesUiState,
    contentPadding: PaddingValues,
    onToggle: (Long, Boolean) -> Unit,
) {
    if (state.loading) {
        LoadingScreen(modifier = Modifier.testTag("modes_loading"))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("modes_list"),
        contentPadding = contentPadding,
    ) {
        item(key = "context") {
            ContextStrip(
                text = state.activePreset?.let { "${it.name} mode on" } ?: "No mode active",
                detail = "today ${TimeUtils.formatDuration(state.screenTime.todayMs)}",
                active = state.activePreset != null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "screen_time") {
            ScreenTimeCard(state.screenTime)
        }
        item(key = "header") {
            SectionHeader(title = "Modes", meta = "${state.presets.size}")
        }
        if (state.presets.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    message = if (state.error) "Couldn't load modes." else "No modes yet.",
                    modifier = Modifier.padding(vertical = 32.dp).testTag("modes_empty"),
                )
            }
        }
        items(state.presets, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                active = preset.id == state.activeId,
                enabled = state.applyingId == null,
                onToggle = { enable -> onToggle(preset.id, enable) },
            )
        }
    }
}

@Composable
private fun ScreenTimeCard(summary: ScreenTimeSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("modes_screen_time"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Stat(label = "Screen time today", value = TimeUtils.formatDuration(summary.todayMs))
            Stat(
                label = "${ModesViewModel.SUMMARY_DAYS}-day average",
                value = if (summary.days == 0) "—" else TimeUtils.formatDuration(summary.avgDailyMs),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(text = value, style = DoormanMonoType.value)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PresetCard(
    preset: SoundPreset,
    active: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("mode_card_${preset.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = preset.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = describe(preset),
                    style = DoormanMonoType.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = active,
                onCheckedChange = onToggle,
                enabled = enabled,
                modifier = Modifier.testTag("mode_toggle_${preset.id}"),
            )
        }
    }
}

private fun describe(preset: SoundPreset): String = buildList {
    add(
        when (preset.ringerMode) {
            RingerMode.SILENT -> "silent"
            RingerMode.VIBRATE -> "vibrate"
            RingerMode.NORMAL -> "ring"
        },
    )
    preset.dndEnabled?.let { add(if (it) "DND on" else "DND off") }
    preset.notificationVolume?.let { add("notif $it%") }
    when (preset.triggerType) {
        PresetTrigger.MANUAL -> Unit
        PresetTrigger.TIME_RANGE -> add("auto: time")
        PresetTrigger.WIFI_CONNECTED -> add("auto: wifi")
    }
}.joinToString(" · ")
