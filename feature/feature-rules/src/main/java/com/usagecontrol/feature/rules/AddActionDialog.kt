package com.usagecontrol.feature.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usagecontrol.core.model.ActionType
import com.usagecontrol.core.model.RuleAction
import com.usagecontrol.core.model.RulePillar

@Composable
fun AddActionDialog(
    pillar: RulePillar,
    onDismiss: () -> Unit,
    onAdd: (RuleAction) -> Unit,
) {
    var selectedType by remember { mutableStateOf<ActionType?>(null) }
    val availableActions = ActionType.entries.filter { it.forPillar(pillar) }

    if (selectedType == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Action") },
            text = {
                LazyColumn {
                    items(availableActions) { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                type.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(type.displayName(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
        )
    } else {
        ActionParamsDialog(
            type = selectedType!!,
            onDismiss = { selectedType = null },
            onConfirm = { params ->
                onAdd(RuleAction(type = selectedType!!, params = params))
            },
        )
    }
}

@Composable
private fun ActionParamsDialog(
    type: ActionType,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    when (type) {
        ActionType.DISMISS_NOTIFICATION,
        ActionType.MUTE_NOTIFICATION -> {
            // No params needed
            LaunchedEffect(Unit) { onConfirm(emptyMap()) }
        }

        ActionType.SNOOZE_NOTIFICATION -> {
            SnoozeParamsDialog(onDismiss = onDismiss, onConfirm = onConfirm)
        }

        ActionType.ADVISE -> {
            AdviseParamsDialog(onDismiss = onDismiss, onConfirm = onConfirm)
        }

        ActionType.SOFT_BLOCK,
        ActionType.HARD_BLOCK -> {
            // No params needed
            LaunchedEffect(Unit) { onConfirm(emptyMap()) }
        }

        ActionType.SET_VOLUME -> {
            VolumeParamsDialog(onDismiss = onDismiss, onConfirm = onConfirm)
        }

        ActionType.SET_RINGER_MODE -> {
            RingerModeDialog(onDismiss = onDismiss, onConfirm = onConfirm)
        }

        ActionType.SET_DND -> {
            DndDialog(onDismiss = onDismiss, onConfirm = onConfirm)
        }

        ActionType.SET_LOCK_TIMEOUT -> {
            LockTimeoutDialog(onDismiss = onDismiss, onConfirm = onConfirm)
        }
    }
}

@Composable
private fun SnoozeParamsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    var minutes by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Snooze Duration") },
        text = {
            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it },
                label = { Text("Minutes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(mapOf("durationMinutes" to minutes)) },
                enabled = minutes.toIntOrNull() != null && minutes.toInt() > 0,
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AdviseParamsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    var limitMinutes by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advisory Limit") },
        text = {
            OutlinedTextField(
                value = limitMinutes,
                onValueChange = { limitMinutes = it },
                label = { Text("Daily limit (minutes)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(mapOf("dailyLimitMinutes" to limitMinutes)) },
                enabled = limitMinutes.toIntOrNull() != null && limitMinutes.toInt() > 0,
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun VolumeParamsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    var stream by remember { mutableStateOf("notification") }
    var level by remember { mutableStateOf(50f) }
    val streams = listOf("alarm", "ring", "notification", "media")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Volume") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stream", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    streams.forEach { s ->
                        FilterChip(
                            selected = stream == s,
                            onClick = { stream = s },
                            label = { Text(s.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Text("Level: ${level.toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = level,
                    onValueChange = { level = it },
                    valueRange = 0f..100f,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(mapOf("stream" to stream, "level" to level.toInt().toString())) },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RingerModeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    var mode by remember { mutableStateOf("normal") }
    val modes = listOf("silent", "vibrate", "normal")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ringer Mode") },
        text = {
            Column {
                modes.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mode = m }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == m, onClick = { mode = m })
                        Spacer(Modifier.width(8.dp))
                        Text(m.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mapOf("mode" to mode)) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DndDialog(
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    var enabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Do Not Disturb") },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enable DND", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mapOf("enabled" to enabled.toString())) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun LockTimeoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    var timeoutSeconds by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lock Timeout") },
        text = {
            OutlinedTextField(
                value = timeoutSeconds,
                onValueChange = { timeoutSeconds = it },
                label = { Text("Timeout (seconds)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val seconds = timeoutSeconds.toIntOrNull() ?: return@TextButton
                    onConfirm(mapOf("timeoutMs" to (seconds * 1000).toString()))
                },
                enabled = timeoutSeconds.toIntOrNull() != null,
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
