package com.usagecontrol.feature.notifications.screener

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.rules.ReScreenPrompt
import com.usagecontrol.core.ui.component.SectionHeader
import com.usagecontrol.feature.notifications.AppIcon
import com.usagecontrol.feature.notifications.lanes.LaneChoiceChip

/**
 * The Screener (R014): every sender no route claims yet, with a sample notification and
 * one-tap lane chips. Tapping a lane creates the route and re-files that sender's history.
 * Habit re-screening prompts (R004) sit at the top. Opened by the `doorman://screener` link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenerSheet(
    focusPackage: String?,
    onDismiss: () -> Unit,
    viewModel: ScreenerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(focusPackage) { viewModel.focus(focusPackage) }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("screener_sheet"),
    ) {
        Box {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "title") {
                    Text(
                        "Screener",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                if (state.prompts.isNotEmpty()) {
                    item(key = "prompts-header") { SectionHeader(title = "Habits", meta = state.prompts.size.toString()) }
                    items(state.prompts, key = { "prompt:${it.packageName}" }) { prompt ->
                        ReScreenPromptCard(
                            prompt = prompt,
                            enabled = "APP|${prompt.packageName}|" !in state.busy,
                            onAccept = { viewModel.acceptPrompt(prompt) },
                            onDismiss = { viewModel.dismissPrompt(prompt) },
                        )
                    }
                }

                item(key = "senders-header") {
                    SectionHeader(title = "New senders", meta = state.senders.size.toString())
                }
                when {
                    state.loading -> item(key = "loading") { Muted("Loading…") }
                    state.error -> item(key = "error") { Muted("Couldn't load the Screener") }
                    state.senders.isEmpty() -> item(key = "empty") {
                        Muted("Everyone has been screened", Modifier.testTag("screener_empty"))
                    }
                }
                items(state.senders, key = { "sender:${it.id}" }) { sender ->
                    SenderCard(
                        sender = sender,
                        lanes = state.lanes,
                        enabled = sender.id !in state.busy,
                        onRoute = { lane -> viewModel.route(sender, lane) },
                    )
                }
                item(key = "bottom-space") { Spacer(Modifier.padding(bottom = 24.dp)) }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).testTag("screener_snackbar"),
            )
        }
    }
}

@Composable
private fun SenderCard(
    sender: UnscreenedSender,
    lanes: List<Lane>,
    enabled: Boolean,
    onRoute: (Lane) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("screener_sender")) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = sender.packageName, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    sender.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (sender.count == 1) "1 alert" else "${sender.count} alerts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val sample = listOfNotNull(
                sender.sampleTitle?.takeIf { it.isNotBlank() },
                sender.sampleText?.takeIf { it.isNotBlank() },
            ).joinToString(": ")
            if (sample.isNotEmpty()) {
                Text(sample, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            lanes.firstOrNull { it.id == sender.currentLaneId }?.let {
                Text(
                    "Filed in ${it.name} for now",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("screener_lanes"),
            ) {
                lanes.forEach { lane ->
                    LaneChoiceChip(
                        lane = lane,
                        selected = false,
                        enabled = enabled,
                        onClick = { onRoute(lane) },
                    )
                }
            }
        }
    }
}

/** A habit-based "move this app?" question; shared by the Screener and the app profile. */
@Composable
fun ReScreenPromptCard(
    prompt: ReScreenPrompt,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("rescreen_prompt")) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(prompt.message, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("rescreen_dismiss")) { Text("Not now") }
                TextButton(
                    onClick = onAccept,
                    enabled = enabled,
                    modifier = Modifier.testTag("rescreen_accept"),
                ) { Text("Move to ${prompt.targetLaneName}") }
            }
        }
    }
}

@Composable
private fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
