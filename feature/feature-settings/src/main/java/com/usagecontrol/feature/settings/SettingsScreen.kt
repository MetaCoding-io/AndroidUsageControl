package com.usagecontrol.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRevisitTour: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val retentionDays by viewModel.retentionDays.collectAsStateWithLifecycle()
    val notificationCount by viewModel.notificationCount.collectAsStateWithLifecycle()
    val ruleCount by viewModel.ruleCount.collectAsStateWithLifecycle()
    val cleanupResult by viewModel.cleanupResult.collectAsStateWithLifecycle()
    val autoCleanupEnabled by viewModel.autoCleanupEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showRetentionPicker by remember { mutableStateOf(false) }
    var exportSnackbar by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Permissions section
            SectionHeader("Permissions")

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notification Access",
                subtitle = "Required for notification capture",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
            )

            SettingsItem(
                icon = Icons.Default.DataUsage,
                title = "Usage Access",
                subtitle = "Required for screen time tracking",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                },
            )

            SettingsItem(
                icon = Icons.Default.DoNotDisturb,
                title = "Do Not Disturb Access",
                subtitle = "Required for DND control",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Data section
            SectionHeader("Data Management")

            SettingsItem(
                icon = Icons.Default.History,
                title = "Data Retention",
                subtitle = "Keep data for $retentionDays days ($notificationCount notifications stored)",
                onClick = { showRetentionPicker = true },
            )

            // Auto cleanup toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setAutoCleanupEnabled(!autoCleanupEnabled) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoDelete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Auto Cleanup", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Run daily cleanup automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = autoCleanupEnabled,
                    onCheckedChange = { viewModel.setAutoCleanupEnabled(it) },
                )
            }

            SettingsItem(
                icon = Icons.Default.CleaningServices,
                title = "Run Cleanup Now",
                subtitle = "Delete data older than $retentionDays days",
                onClick = { viewModel.runCleanup() },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Export section
            SectionHeader("Export & Backup")

            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = "Export Rules",
                subtitle = "$ruleCount rules - Copy as JSON to clipboard",
                onClick = {
                    viewModel.exportRulesJson { json ->
                        clipboardManager.setText(AnnotatedString(json))
                        exportSnackbar = "Rules copied to clipboard"
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Help section
            SectionHeader("Help")

            SettingsItem(
                icon = Icons.Default.Explore,
                title = "Revisit tour",
                subtitle = "View the feature tour again",
                onClick = onRevisitTour,
                modifier = Modifier.testTag("settings_item_revisit_tour"),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About section
            SectionHeader("About")

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_app_name),
                subtitle = "v0.1.0 - All data stored locally on device",
                onClick = {},
            )

            SettingsItem(
                icon = Icons.Default.Business,
                title = "Developed by",
                subtitle = "Metacoding Solutions LLC",
                onClick = {},
            )

            SettingsItem(
                icon = Icons.Default.Copyright,
                title = "Copyright",
                subtitle = "\u00a9 2026 Metacoding Solutions LLC. All rights reserved.",
                onClick = {},
            )
        }
    }

    // Retention picker dialog
    if (showRetentionPicker) {
        RetentionPickerDialog(
            currentDays = retentionDays,
            onDismiss = { showRetentionPicker = false },
            onConfirm = {
                viewModel.setRetentionDays(it)
                showRetentionPicker = false
            },
        )
    }

    // Cleanup result snackbar
    cleanupResult?.let { result ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearCleanupResult() }) {
                    Text("OK")
                }
            },
        ) {
            Text(result)
        }
    }

    // Export snackbar
    exportSnackbar?.let { msg ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { exportSnackbar = null }) {
                    Text("OK")
                }
            },
        ) {
            Text(msg)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RetentionPickerDialog(
    currentDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val options = listOf(7, 14, 30, 60, 90, 180, 365)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Data Retention Period") },
        text = {
            Column {
                options.forEach { days ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(days) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentDays == days,
                            onClick = { onConfirm(days) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when {
                                days < 30 -> "$days days"
                                days < 365 -> "${days / 30} months"
                                else -> "1 year"
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
