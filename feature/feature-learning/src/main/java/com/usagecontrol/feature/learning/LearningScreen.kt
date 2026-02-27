package com.usagecontrol.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.model.AppDismissStats
import com.usagecontrol.core.ui.component.EmptyState
import com.usagecontrol.core.ui.component.LoadingScreen
import com.usagecontrol.core.ui.component.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    viewModel: LearningViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val dayRange by viewModel.dayRange.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Insights") })

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
        } else if (stats.isEmpty()) {
            EmptyState("No notification data yet. Stats will appear as notifications are captured.")
        } else {
            // Summary stats
            val totalNotifs = stats.sumOf { it.totalNotifications }
            val totalDismissed = stats.sumOf { it.dismissedCount }
            val totalOpened = stats.sumOf { it.openedCount }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = "Total",
                    value = "$totalNotifs",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Dismissed",
                    value = "$totalDismissed",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Opened",
                    value = "$totalOpened",
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Per-App Breakdown",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(stats) { stat ->
                    AppStatsItem(stat)
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
                    text = "Dismiss rate: ${(stat.dismissRate * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(120.dp),
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
                    text = "Open rate: ${(stat.openRate * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(120.dp),
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

            // Suggestion hint
            if (stat.dismissRate > 0.9f && stat.totalNotifications >= 10) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "You dismiss most notifications from this app. Consider adding an auto-dismiss rule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}
