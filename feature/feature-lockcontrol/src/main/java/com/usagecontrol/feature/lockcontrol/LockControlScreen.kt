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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.model.ContextSnapshot
import com.usagecontrol.core.model.PresetTrigger
import com.usagecontrol.core.model.RingerMode
import com.usagecontrol.core.model.SoundPreset
import com.usagecontrol.core.ui.component.DegradedPermissionBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockControlScreen(
    onNavigateToRules: () -> Unit = {},
    viewModel: LockControlViewModel = hiltViewModel(),
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val currentContext by viewModel.currentContext.collectAsStateWithLifecycle()
    val showPermissionBanner by viewModel.showPermissionBanner.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recheckPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Controls") },
            actions = {
                TextButton(onClick = onNavigateToRules) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Rules")
                }
                IconButton(onClick = { showCustomDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add preset")
                }
            }
        )

        if (showPermissionBanner) {
            DegradedPermissionBanner(
                message = "Do Not Disturb access is not granted. Sound presets and rules won't work until you enable it in Settings.",
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Current context card
            item {
                CurrentContextCard(
                    context = currentContext,
                    onRefresh = { viewModel.refreshContext() },
                )
            }

            // Quick setup section
            item {
                Text(
                    text = "Quick Setup",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val hasNightPreset = presets.any { it.name == "Night Mode" }
                    val hasMeetingPreset = presets.any { it.name == "Meeting Mode" }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.createNightPreset() },
                            enabled = !hasNightPreset,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (hasNightPreset) "Night Added" else "Night Mode")
                        }
                        OutlinedButton(
                            onClick = { viewModel.createMeetingPreset() },
                            enabled = !hasMeetingPreset,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (hasMeetingPreset) "Meeting Added" else "Meeting Mode")
                        }
                    }

                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add WiFi-based Preset")
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(
                    text = "Sound & Lock Presets",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (presets.isEmpty()) {
                item {
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
                }
            } else {
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

    if (showCustomDialog) {
        CustomPresetDialog(
            onDismiss = { showCustomDialog = false },
            onCreate = { name, ringerMode, alarm, ring, notif, media, dnd, trigger, params ->
                viewModel.createCustomPreset(name, ringerMode, alarm, ring, notif, media, dnd, trigger, params)
                showCustomDialog = false
            },
        )
    }
}

@Composable
private fun CurrentContextCard(
    context: ContextSnapshot?,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Sensors,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Current Context",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (context != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ContextChip(Icons.Default.Wifi, context.wifiSsid ?: "No WiFi")
                    ContextChip(
                        Icons.Default.BatteryChargingFull,
                        if (context.isCharging) "Charging" else "Battery",
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ContextChip(Icons.Default.Bluetooth, context.bluetoothDevice ?: "No BT")
                    ContextChip(Icons.Default.Event, context.calendarEvent ?: "No event")
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No context data yet. Start the monitoring service.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun ContextChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 1,
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
                    if (preset.mediaVolume != null) append(" | Media: ${preset.mediaVolume}%")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomPresetDialog(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        ringerMode: RingerMode,
        alarmVolume: Int?,
        ringVolume: Int?,
        notificationVolume: Int?,
        mediaVolume: Int?,
        dndEnabled: Boolean?,
        triggerType: PresetTrigger,
        triggerParams: Map<String, String>,
    ) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var ringerMode by remember { mutableStateOf(RingerMode.NORMAL) }
    var alarmVolume by remember { mutableStateOf("") }
    var mediaVolume by remember { mutableStateOf("") }
    var dndEnabled by remember { mutableStateOf(false) }
    var triggerType by remember { mutableStateOf(PresetTrigger.MANUAL) }
    var startTime by remember { mutableStateOf("22:00") }
    var endTime by remember { mutableStateOf("07:00") }
    var wifiSsid by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Ringer mode
                Text("Ringer Mode", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RingerMode.entries.forEach { mode ->
                        FilterChip(
                            selected = ringerMode == mode,
                            onClick = { ringerMode = mode },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }

                // Volume controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alarmVolume,
                        onValueChange = { alarmVolume = it.filter { c -> c.isDigit() } },
                        label = { Text("Alarm %") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = mediaVolume,
                        onValueChange = { mediaVolume = it.filter { c -> c.isDigit() } },
                        label = { Text("Media %") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }

                // DND
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dndEnabled, onCheckedChange = { dndEnabled = it })
                    Text("Enable Do Not Disturb")
                }

                // Trigger type
                Text("Trigger", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetTrigger.entries.forEach { trigger ->
                        FilterChip(
                            selected = triggerType == trigger,
                            onClick = { triggerType = trigger },
                            label = {
                                Text(
                                    when (trigger) {
                                        PresetTrigger.TIME_RANGE -> "Time"
                                        PresetTrigger.WIFI_CONNECTED -> "WiFi"
                                        PresetTrigger.MANUAL -> "Manual"
                                    }
                                )
                            },
                        )
                    }
                }

                // Trigger params
                when (triggerType) {
                    PresetTrigger.TIME_RANGE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Start (HH:mm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("End (HH:mm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                        }
                    }
                    PresetTrigger.WIFI_CONNECTED -> {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("WiFi SSID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    PresetTrigger.MANUAL -> { /* No params needed */ }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val params = when (triggerType) {
                        PresetTrigger.TIME_RANGE -> mapOf("startTime" to startTime, "endTime" to endTime)
                        PresetTrigger.WIFI_CONNECTED -> mapOf("ssid" to wifiSsid)
                        PresetTrigger.MANUAL -> emptyMap()
                    }
                    onCreate(
                        name,
                        ringerMode,
                        alarmVolume.toIntOrNull(),
                        null, // ring volume
                        null, // notification volume
                        mediaVolume.toIntOrNull(),
                        if (dndEnabled) true else null,
                        triggerType,
                        params,
                    )
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
