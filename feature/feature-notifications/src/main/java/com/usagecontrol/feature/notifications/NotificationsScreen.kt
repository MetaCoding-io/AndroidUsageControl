package com.usagecontrol.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.model.NotificationAction
import com.usagecontrol.core.model.NotificationRecord
import com.usagecontrol.core.ui.component.EmptyState

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

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("Notifications") },
            actions = {
                TextButton(onClick = onNavigateToRules) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Rules (${rules.size})")
                }
            }
        )

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

        // Notification list
        if (notifications.isEmpty()) {
            EmptyState("No notifications captured yet")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
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

            if (notification.title != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (notification.text != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Action badge
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

