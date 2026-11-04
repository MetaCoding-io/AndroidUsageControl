package com.usagecontrol.feature.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PermissionWalkthroughScreen(
    onComplete: () -> Unit,
    viewModel: PermissionWalkthroughViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // ON_RESUME permission re-check (Pattern 2 from research)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recheckAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Navigate to main app when walkthrough completes
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    PermissionWalkthroughContent(
        state = state,
        onOpenSettings = { step ->
            when (step) {
                PermissionStep.NotificationAccess -> context.openNotificationListenerSettings()
                PermissionStep.UsageAccess -> context.openUsageAccessSettings()
                PermissionStep.DndAccess -> context.openDndAccessSettings()
            }
        },
        onContinue = { viewModel.advanceStep() },
        onSkip = { viewModel.advanceStep() },
    )
}

@Composable
private fun PermissionWalkthroughContent(
    state: PermissionWalkthroughState,
    onOpenSettings: (PermissionStep) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Step counter header (PERM-04)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Set up permissions",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${state.currentStep + 1} / ${PermissionStep.TOTAL_STEPS}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { (state.currentStep + 1).toFloat() / PermissionStep.TOTAL_STEPS.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Step content (PERM-01, PERM-03)
            val currentPermissionStep = PermissionStep.fromIndex(state.currentStep)
            val isGranted = state.permissionStates[state.currentStep]

            PermissionStepContent(
                step = currentPermissionStep,
                isGranted = isGranted,
                onOpenSettings = { onOpenSettings(currentPermissionStep) },
                onContinue = onContinue,
                onSkip = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun PermissionStepContent(
    step: PermissionStep,
    isGranted: Boolean,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Priming content per step (PERM-01) -- user-facing strings in composable, not ViewModel
    val appName = stringResource(R.string.onboarding_app_name)
    val (title, explanation, hint) = when (step) {
        PermissionStep.NotificationAccess -> Triple(
            "Notification Access",
            stringResource(R.string.onboarding_perm_notification_explanation, appName),
            stringResource(R.string.onboarding_perm_settings_hint, appName),
        )
        PermissionStep.UsageAccess -> Triple(
            "Usage Access",
            stringResource(R.string.onboarding_perm_usage_explanation, appName),
            stringResource(R.string.onboarding_perm_settings_hint, appName),
        )
        PermissionStep.DndAccess -> Triple(
            "Do Not Disturb Access",
            stringResource(R.string.onboarding_perm_dnd_explanation, appName),
            stringResource(R.string.onboarding_perm_settings_hint, appName),
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Granted state: checkmark + Continue (PERM-03)
        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Permission granted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Access granted",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().testTag("permission_button_continue"),
            ) {
                Text("Continue")
            }
        } else {
            // Not granted: priming CTA + skip option
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().testTag("permission_button_grant"),
            ) {
                Text("Grant Access")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().testTag("permission_button_skip"),
            ) {
                Text("Skip for now")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- Settings intent helpers (Pattern 3 from research) ---

/** Open Notification Listener Settings. API 30+: opens directly to this app's toggle. */
private fun Context.openNotificationListenerSettings() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(
                        packageName,
                        "com.usagecontrol.service.notification.NotificationCaptureService",
                    ).flattenToString(),
                )
            })
        } else {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    } catch (_: Exception) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }
}

/** Open Usage Access Settings. No package URI -- opens global list (Pitfall 3 from research). */
private fun Context.openUsageAccessSettings() {
    try {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    } catch (_: Exception) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }
}

/** Open DND Access Settings. No package URI -- opens global list. */
private fun Context.openDndAccessSettings() {
    try {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    } catch (_: Exception) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }
}
