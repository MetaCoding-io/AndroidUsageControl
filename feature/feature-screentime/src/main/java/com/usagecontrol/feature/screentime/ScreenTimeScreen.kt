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
import com.usagecontrol.core.database.dao.DailyUsageTotalRow
import com.usagecontrol.core.model.UsageRecord
import com.usagecontrol.core.ui.component.EmptyState
import com.usagecontrol.core.ui.component.SimpleBarChart
import com.usagecontrol.core.ui.component.SimpleLineChart
import com.usagecontrol.core.ui.component.StatCard
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
    val dailyTotals by viewModel.dailyTotals.collectAsStateWithLifecycle()
    val trendDays by viewModel.trendDays.collectAsStateWithLifecycle()

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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Total screen time card
            item {
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
            }

            // Trend section
            item {
                ScreenTimeTrend(
                    dailyTotals = dailyTotals,
                    selectedDays = trendDays,
                    onDaysChanged = { viewModel.setTrendDays(it) },
                )
            }

            // App usage list
            if (usageRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No usage data yet. Grant Usage Access permission in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Today's Apps",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                val maxUsage = usageRecords.maxOfOrNull { it.foregroundTimeMs } ?: 1L

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
private fun ScreenTimeTrend(
    dailyTotals: List<DailyUsageTotalRow>,
    selectedDays: Int,
    onDaysChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Usage Trend",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(7 to "7d", 14 to "14d", 30 to "30d").forEach { (days, label) ->
                    FilterChip(
                        selected = selectedDays == days,
                        onClick = { onDaysChanged(days) },
                        label = { Text(label) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (dailyTotals.isNotEmpty()) {
            // Summary stats row
            val avgMs = dailyTotals.map { it.totalMs }.average().toLong()
            val maxDay = dailyTotals.maxByOrNull { it.totalMs }
            val totalApps = dailyTotals.sumOf { it.appCount }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(label = "Daily Avg", value = TimeUtils.formatDuration(avgMs), modifier = Modifier.weight(1f))
                StatCard(
                    label = "Peak Day",
                    value = maxDay?.let { TimeUtils.formatDuration(it.totalMs) } ?: "-",
                    modifier = Modifier.weight(1f),
                )
                StatCard(label = "Total Apps", value = "$totalApps", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            // Bar chart - daily screen time
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Daily Screen Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    SimpleBarChart(
                        values = dailyTotals.map { it.totalMs / 60_000f }, // minutes
                        labels = dailyTotals.map { it.date.takeLast(5) },
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    text = "No historical data yet",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
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
