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
import com.usagecontrol.core.model.ConditionType
import com.usagecontrol.core.model.RuleCondition

@Composable
fun AddConditionDialog(
    onDismiss: () -> Unit,
    onAdd: (RuleCondition) -> Unit,
) {
    var selectedType by remember { mutableStateOf<ConditionType?>(null) }

    if (selectedType == null) {
        // Step 1: Pick condition type
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Condition") },
            text = {
                LazyColumn {
                    items(ConditionType.entries) { type ->
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
                                tint = MaterialTheme.colorScheme.primary,
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
        // Step 2: Configure params
        ConditionParamsDialog(
            type = selectedType!!,
            onDismiss = { selectedType = null },
            onConfirm = { params ->
                onAdd(RuleCondition(type = selectedType!!, params = params))
            },
        )
    }
}

@Composable
private fun ConditionParamsDialog(
    type: ConditionType,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    when (type) {
        ConditionType.APP_PACKAGE -> {
            SingleFieldDialog(
                title = "App Package",
                fieldLabel = "Package name",
                fieldPlaceholder = "com.example.app",
                onDismiss = onDismiss,
                onConfirm = { onConfirm(mapOf("packageName" to it)) },
            )
        }

        ConditionType.TIME_RANGE -> {
            TwoFieldDialog(
                title = "Time Range",
                field1Label = "Start time",
                field1Placeholder = "09:00",
                field2Label = "End time",
                field2Placeholder = "17:00",
                onDismiss = onDismiss,
                onConfirm = { start, end -> onConfirm(mapOf("startTime" to start, "endTime" to end)) },
            )
        }

        ConditionType.DAY_OF_WEEK -> {
            DayOfWeekDialog(
                onDismiss = onDismiss,
                onConfirm = { days -> onConfirm(mapOf("days" to days)) },
            )
        }

        ConditionType.WIFI_SSID -> {
            SingleFieldDialog(
                title = "WiFi Network",
                fieldLabel = "SSID",
                fieldPlaceholder = "MyHomeWifi",
                onDismiss = onDismiss,
                onConfirm = { onConfirm(mapOf("ssid" to it)) },
            )
        }

        ConditionType.KEYWORD_MATCH -> {
            KeywordDialog(
                onDismiss = onDismiss,
                onConfirm = { keyword, field -> onConfirm(mapOf("keyword" to keyword, "field" to field)) },
            )
        }

        ConditionType.NOTIFICATION_PRIORITY -> {
            SingleFieldDialog(
                title = "Min Notification Priority",
                fieldLabel = "Minimum priority (0-5)",
                fieldPlaceholder = "0",
                onDismiss = onDismiss,
                onConfirm = { onConfirm(mapOf("minPriority" to it)) },
            )
        }

        ConditionType.APP_CATEGORY -> {
            CategoryDialog(
                onDismiss = onDismiss,
                onConfirm = { onConfirm(mapOf("category" to it)) },
            )
        }

        ConditionType.CHARGING -> {
            ChargingDialog(
                onDismiss = onDismiss,
                onConfirm = { onConfirm(mapOf("isCharging" to it.toString())) },
            )
        }

        ConditionType.BLUETOOTH_DEVICE -> {
            SingleFieldDialog(
                title = "Bluetooth Device",
                fieldLabel = "Device name",
                fieldPlaceholder = "Car Stereo",
                onDismiss = onDismiss,
                onConfirm = { onConfirm(mapOf("deviceName" to it)) },
            )
        }

        ConditionType.CALENDAR_EVENT -> {
            SingleFieldDialog(
                title = "Calendar Event",
                fieldLabel = "Event keyword",
                fieldPlaceholder = "meeting",
                onDismiss = onDismiss,
                onConfirm = { onConfirm(mapOf("keyword" to it)) },
            )
        }
    }
}

@Composable
private fun SingleFieldDialog(
    title: String,
    fieldLabel: String,
    fieldPlaceholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(fieldLabel) },
                placeholder = { Text(fieldPlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TwoFieldDialog(
    title: String,
    field1Label: String,
    field1Placeholder: String,
    field2Label: String,
    field2Placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value1,
                    onValueChange = { value1 = it },
                    label = { Text(field1Label) },
                    placeholder = { Text(field1Placeholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = value2,
                    onValueChange = { value2 = it },
                    label = { Text(field2Label) },
                    placeholder = { Text(field2Placeholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value1.trim(), value2.trim()) },
                enabled = value1.isNotBlank() && value2.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DayOfWeekDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Days of Week") },
        text = {
            Column {
                days.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected[day] = !(selected[day] ?: false) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selected[day] ?: false,
                            onCheckedChange = { selected[day] = it },
                        )
                        Text(day)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDays = days.filter { selected[it] == true }.joinToString(",")
                    if (selectedDays.isNotBlank()) onConfirm(selectedDays)
                },
                enabled = selected.any { it.value },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun KeywordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var field by remember { mutableStateOf("both") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keyword Match") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Keyword") },
                    placeholder = { Text("sale") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Search in:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("title", "text", "both").forEach { option ->
                        FilterChip(
                            selected = field == option,
                            onClick = { field = option },
                            label = { Text(option.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (keyword.isNotBlank()) onConfirm(keyword.trim(), field) },
                enabled = keyword.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val categories = listOf("social", "game", "productivity", "communication", "entertainment", "news", "other")
    var selected by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Category") },
        text = {
            Column {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = category }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == category,
                            onClick = { selected = category },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(category.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selected.isNotBlank()) onConfirm(selected) },
                enabled = selected.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ChargingDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var isCharging by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charging State") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCharging = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isCharging, onClick = { isCharging = true })
                    Spacer(Modifier.width(8.dp))
                    Text("Charging")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCharging = false }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = !isCharging, onClick = { isCharging = false })
                    Spacer(Modifier.width(8.dp))
                    Text("Not charging")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(isCharging) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
