package com.usagecontrol.feature.lockcontrol

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.model.PresetTrigger
import com.usagecontrol.core.model.RingerMode
import com.usagecontrol.core.model.SoundPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockControlScreen(
    onNavigateToRules: () -> Unit = {},
    viewModel: LockControlViewModel = hiltViewModel(),
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Controls") },
            actions = {
                TextButton(onClick = onNavigateToRules) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Rules")
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add preset")
                }
            }
        )

        // Quick actions
        Text(
            text = "Quick Setup",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val hasNightPreset = presets.any { it.name == "Night Mode" }
            OutlinedButton(
                onClick = { viewModel.createNightPreset() },
                enabled = !hasNightPreset,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.NightsStay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (hasNightPreset) "Night Mode Added" else "Add Night Mode")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Presets list
        Text(
            text = "Sound & Lock Presets",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (presets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No presets yet. Use Quick Setup or tap + to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(presets, key = { it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        onToggle = { viewModel.togglePreset(preset) },
                        onDelete = { viewModel.deletePreset(preset) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddWifiPresetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { ssid ->
                viewModel.createHomePreset(ssid)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun PresetCard(
    preset: SoundPreset,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (preset.triggerType) {
                    PresetTrigger.TIME_RANGE -> Icons.Default.Schedule
                    PresetTrigger.WIFI_CONNECTED -> Icons.Default.Wifi
                    PresetTrigger.MANUAL -> Icons.Default.TouchApp
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                val triggerText = when (preset.triggerType) {
                    PresetTrigger.TIME_RANGE -> {
                        val start = preset.triggerParams["startTime"] ?: "?"
                        val end = preset.triggerParams["endTime"] ?: "?"
                        "Active $start - $end"
                    }
                    PresetTrigger.WIFI_CONNECTED -> "When on WiFi: ${preset.triggerParams["ssid"] ?: "?"}"
                    PresetTrigger.MANUAL -> "Manual activation"
                }
                Text(
                    text = triggerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val modeText = buildString {
                    append("Ringer: ${preset.ringerMode.name.lowercase()}")
                    if (preset.dndEnabled == true) append(" | DND on")
                    if (preset.alarmVolume != null) append(" | Alarm: ${preset.alarmVolume}%")
                }
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = preset.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun AddWifiPresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var ssid by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Home WiFi Preset") },
        text = {
            Column {
                Text("When connected to this WiFi, the phone will stay in normal mode with DND off.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("WiFi network name (SSID)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(ssid) },
                enabled = ssid.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
