package com.usagecontrol.feature.learning

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.model.AppDismissStats
import com.usagecontrol.core.model.Suggestion
import com.usagecontrol.core.model.SuggestionType
import com.usagecontrol.core.ui.component.EmptyState
import com.usagecontrol.core.ui.component.LoadingScreen
import com.usagecontrol.core.ui.component.SimpleBarChart
import com.usagecontrol.core.ui.component.SimpleLineChart
import com.usagecontrol.core.ui.component.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    viewModel: LearningViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val dayRange by viewModel.dayRange.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val hourlyData by viewModel.hourlyData.collectAsStateWithLifecycle()
    val dailyTrend by viewModel.dailyTrend.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Insights") },
            actions = {
                TextButton(
                    onClick = { viewModel.runAnalysis() },
                    enabled = !isAnalyzing,
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Analyze")
                }
            }
        )

        // Time range selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1 to "Today", 7 to "7 days", 30 to "30 days").forEach { (days, label) ->
                FilterChip(
                    selected = dayRange == days,
                    onClick = { viewModel.setDayRange(days) },
                    label = { Text(label) },
                )
            }
        }

        if (isLoading) {
            LoadingScreen()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                // Suggestions section
                if (suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Suggestions",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(suggestions, key = { it.id }) { suggestion ->
                        SuggestionCard(
                            suggestion = suggestion,
                            onAccept = { viewModel.acceptSuggestion(suggestion) },
                            onDismiss = { viewModel.dismissSuggestion(suggestion) },
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                }

                // Stats section
                if (stats.isEmpty()) {
                    item {
                        EmptyState("No notification data yet. Stats will appear as notifications are captured.")
                    }
                } else {
                    // Summary stats
                    item {
                        val totalNotifs = stats.sumOf { it.totalNotifications }
                        val totalDismissed = stats.sumOf { it.dismissedCount }
                        val totalOpened = stats.sumOf { it.openedCount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatCard(label = "Total", value = "$totalNotifs", modifier = Modifier.weight(1f))
                            StatCard(label = "Dismissed", value = "$totalDismissed", modifier = Modifier.weight(1f))
                            StatCard(label = "Opened", value = "$totalOpened", modifier = Modifier.weight(1f))
                        }
                    }

                    // Charts section
                    item {
                        Spacer(Modifier.height(8.dp))
                        InsightsCharts(
                            hourlyData = hourlyData,
                            dailyTrend = dailyTrend,
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Per-App Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    items(stats) { stat ->
                        AppStatsItem(stat)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsCharts(
    hourlyData: List<Float>,
    dailyTrend: List<com.usagecontrol.core.database.dao.DailyNotifStatsRow>,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Daily notification trend
        if (dailyTrend.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Notification Trend",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        dataPoints = dailyTrend.map { it.total.toFloat() },
                        labels = dailyTrend.map { it.date.takeLast(5) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Hourly distribution
        if (hourlyData.any { it > 0f }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Dismissals by Hour",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    // Show every 3 hours for readability
                    val labels = (0..23).map { h ->
                        if (h % 3 == 0) "${h}h" else ""
                    }
                    val peakHour = hourlyData.indices.maxByOrNull { hourlyData[it] } ?: -1
                    SimpleBarChart(
                        values = hourlyData,
                        labels = labels,
                        highlightIndex = peakHour,
                        highlightColor = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: Suggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = when (suggestion.type) {
                        SuggestionType.AUTO_DISMISS_APP -> Icons.Default.NotificationsOff
                        SuggestionType.QUICK_DISMISS_APP -> Icons.Default.Speed
                        SuggestionType.TIME_BASED_DISMISS -> Icons.Default.Schedule
                        SuggestionType.SCREEN_TIME_LIMIT -> Icons.Default.Timer
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.ruleTemplate.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = suggestion.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    )
                }
            }

            // Confidence indicator
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Confidence: ${(suggestion.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { suggestion.confidence },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                )
            }

            // Action buttons
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onAccept) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Create Rule")
                }
            }
        }
    }
}

@Composable
private fun AppStatsItem(stat: AppDismissStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stat.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${stat.totalNotifications} notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Dismiss rate bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Dismiss: ${(stat.dismissRate * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(100.dp),
                )
                LinearProgressIndicator(
                    progress = { stat.dismissRate },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = if (stat.dismissRate > 0.8f)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Open rate bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Open: ${(stat.openRate * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(100.dp),
                )
                LinearProgressIndicator(
                    progress = { stat.openRate },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Auto-dismissed count
            if (stat.autoDismissedCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stat.autoDismissedCount} auto-dismissed by rules",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
