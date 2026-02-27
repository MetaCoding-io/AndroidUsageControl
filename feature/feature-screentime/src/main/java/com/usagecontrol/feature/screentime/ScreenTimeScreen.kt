package com.usagecontrol.feature.screentime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.model.UsageRecord
import com.usagecontrol.core.ui.component.EmptyState
import com.usagecontrol.core.ui.component.UsageBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTimeScreen(
    onNavigateToRules: () -> Unit = {},
    viewModel: ScreenTimeViewModel = hiltViewModel(),
) {
    val usageRecords by viewModel.usageRecords.collectAsStateWithLifecycle()
    val totalScreenTime by viewModel.totalScreenTime.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()

    var showLimitDialog by remember { mutableStateOf<UsageRecord?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Screen Time") },
            actions = {
                TextButton(onClick = onNavigateToRules) {
                    Icon(Icons.Default.Timer, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Limits (${rules.size})")
                }
            }
        )

        // Total screen time card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Today's Screen Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = TimeUtils.formatDuration(totalScreenTime),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${usageRecords.size} apps used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }

        // App usage list
        if (usageRecords.isEmpty()) {
            EmptyState("No usage data yet. Grant Usage Access permission in Settings.")
        } else {
            val maxUsage = usageRecords.maxOfOrNull { it.foregroundTimeMs } ?: 1L

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(usageRecords, key = { it.id }) { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UsageBar(
                            label = record.appName,
                            usageText = TimeUtils.formatDuration(record.foregroundTimeMs),
                            progress = record.foregroundTimeMs.toFloat() / maxUsage,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showLimitDialog = record }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Set limit",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    // Time limit dialog
    showLimitDialog?.let { record ->
        TimeLimitDialog(
            appName = record.appName,
            onDismiss = { showLimitDialog = null },
            onConfirm = { minutes ->
                viewModel.createTimeLimitRule(record.appName, record.packageName, minutes)
                showLimitDialog = null
            },
        )
    }
}

@Composable
private fun TimeLimitDialog(
    appName: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var minutes by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Limit") },
        text = {
            Column {
                Text("Set a daily usage limit for $appName")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Minutes per day") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { minutes.toIntOrNull()?.let { onConfirm(it) } },
                enabled = minutes.toIntOrNull() != null && minutes.toInt() > 0,
            ) {
                Text("Set Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
