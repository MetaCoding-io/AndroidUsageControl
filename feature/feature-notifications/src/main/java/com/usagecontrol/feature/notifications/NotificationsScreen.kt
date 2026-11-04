package com.usagecontrol.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.model.NotificationAction
import com.usagecontrol.core.model.NotificationRecord
import com.usagecontrol.core.ui.component.DegradedPermissionBanner
import com.usagecontrol.core.ui.component.DonutChart
import com.usagecontrol.core.ui.component.EmptyState
import com.usagecontrol.core.ui.component.SimpleLineChart
import com.usagecontrol.core.ui.component.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateToRules: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val actionBreakdown by viewModel.actionBreakdown.collectAsStateWithLifecycle()
    val dashboardDays by viewModel.dashboardDays.collectAsStateWithLifecycle()
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

    var showDashboard by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notifications") },
            actions = {
                IconButton(onClick = { showDashboard = !showDashboard }) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = "Toggle dashboard",
                        tint = if (showDashboard) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onNavigateToRules) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Rules (${rules.size})")
                }
            }
        )

        if (showPermissionBanner) {
            DegradedPermissionBanner(
                message = "Notification access is not granted. Notification tracking and rules won't work until you enable it in Settings.",
            )
        }

        // App filter chips
        if (apps.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedApp == null,
                        onClick = { viewModel.selectApp(null) },
                        label = { Text("All") },
                    )
                }
                items(apps) { app ->
                    FilterChip(
                        selected = selectedApp == app.packageName,
                        onClick = { viewModel.selectApp(app.packageName) },
                        label = { Text(app.appName) },
                    )
                }
            }
        }

        if (notifications.isEmpty() && !showDashboard) {
            EmptyState("No notifications captured yet")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                // Dashboard section
                if (showDashboard) {
                    item {
                        NotificationDashboard(
                            dailyStats = dailyStats,
                            actionBreakdown = actionBreakdown,
                            selectedDays = dashboardDays,
                            onDaysChanged = { viewModel.setDashboardDays(it) },
                        )
                    }
                }

                // Notification list
                items(notifications, key = { it.id }) { notification ->
                    NotificationItem(
                        notification = notification,
                        onCreateRule = {
                            viewModel.createDismissRule(notification.appName, notification.packageName)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationDashboard(
    dailyStats: List<com.usagecontrol.core.database.dao.DailyNotifStatsRow>,
    actionBreakdown: com.usagecontrol.core.database.dao.ActionBreakdownRow?,
    selectedDays: Int,
    onDaysChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Day range selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7 to "7d", 14 to "14d", 30 to "30d").forEach { (days, label) ->
                FilterChip(
                    selected = selectedDays == days,
                    onClick = { onDaysChanged(days) },
                    label = { Text(label) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Stats summary
        if (actionBreakdown != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(label = "Total", value = "${actionBreakdown.total}", modifier = Modifier.weight(1f))
                StatCard(label = "Dismissed", value = "${actionBreakdown.dismissed}", modifier = Modifier.weight(1f))
                StatCard(label = "Opened", value = "${actionBreakdown.opened}", modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Trend chart + donut side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Line chart - daily notification trend
            if (dailyStats.isNotEmpty()) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Daily Trend",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        SimpleLineChart(
                            dataPoints = dailyStats.map { it.total.toFloat() },
                            labels = dailyStats.map { it.date.takeLast(5) },
                        )
                    }
                }
            }

            // Donut chart - action breakdown
            if (actionBreakdown != null && actionBreakdown.total > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        DonutChart(
                            segments = listOf(
                                actionBreakdown.dismissed.toFloat() to MaterialTheme.colorScheme.error,
                                actionBreakdown.opened.toFloat() to MaterialTheme.colorScheme.primary,
                                actionBreakdown.autoDismissed.toFloat() to MaterialTheme.colorScheme.tertiary,
                                actionBreakdown.pending.toFloat() to MaterialTheme.colorScheme.outline,
                            ),
                            centerText = "${actionBreakdown.total}",
                            modifier = Modifier.size(100.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationRecord,
    onCreateRule: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = when (notification.userAction) {
                NotificationAction.AUTO_DISMISSED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                NotificationAction.DISMISSED -> MaterialTheme.colorScheme.surface
                NotificationAction.OPENED -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface
            }
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notification.appName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = TimeUtils.formatDisplay(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val title = notification.title
            if (title != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val text = notification.text
            if (text != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val actionText = when (notification.userAction) {
                    NotificationAction.DISMISSED -> "Dismissed"
                    NotificationAction.OPENED -> "Opened"
                    NotificationAction.AUTO_DISMISSED -> "Auto-dismissed"
                    NotificationAction.SNOOZED -> "Snoozed"
                    NotificationAction.ARCHIVED -> "Archived"
                    NotificationAction.NONE -> "Pending"
                }
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                if (expanded) {
                    TextButton(onClick = onCreateRule) {
                        Text("Auto-dismiss this app")
                    }
                }
            }
        }
    }
}
