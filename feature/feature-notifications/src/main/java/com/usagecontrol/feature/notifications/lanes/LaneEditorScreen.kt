package com.usagecontrol.feature.notifications.lanes

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.model.LanePolicy
import com.usagecontrol.core.ui.component.LoadingScreen
import com.usagecontrol.core.ui.component.SectionHeader

/** Edits a lane's name, colour, policy, decay, sound and breakthroughs (R014). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaneEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: LaneEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.done.collect { onNavigateBack() }
    }

    Column(modifier = Modifier.fillMaxSize().testTag("lane_editor")) {
        TopAppBar(
            title = { Text(if (state.isNew) "New lane" else "Edit lane") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("lane_editor_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(
                    onClick = viewModel::save,
                    enabled = !state.loading && !state.saving && !state.notFound,
                    modifier = Modifier.testTag("lane_editor_save"),
                ) { Text("Save") }
            },
        )
        when {
            state.loading -> LoadingScreen()
            state.notFound -> Text(
                "This lane no longer exists",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> EditorForm(state, viewModel)
        }
    }
}

@Composable
private fun EditorForm(state: LaneEditorUiState, viewModel: LaneEditorViewModel) {
    val draft = state.draft
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v.take(LaneEditorLogic.MAX_NAME_LENGTH + 8)) } },
            label = { Text("Name") },
            singleLine = true,
            isError = state.error != null && draft.name.isBlank(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("lane_editor_name"),
        )

        SectionHeader(title = "Colour")
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).testTag("lane_editor_colors"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LaneEditorViewModel.palette.forEachIndexed { index, argb ->
                val selected = argb == draft.colorArgb
                LaneSwatch(
                    color = Color(argb),
                    size = 32,
                    modifier = Modifier
                        .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { viewModel.update { it.copy(colorArgb = argb) } }
                        .semantics {
                            this.selected = selected
                            contentDescription = "Colour ${index + 1}"
                        }
                        .testTag("lane_editor_color_$index"),
                )
            }
        }

        SectionHeader(title = "Delivery")
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).testTag("lane_editor_policy"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LanePolicy.entries.forEach { policy ->
                FilterChip(
                    selected = draft.policy == policy,
                    onClick = { viewModel.update { it.copy(policy = policy) } },
                    label = { Text(policy.name) },
                    modifier = Modifier.testTag("lane_editor_policy_${policy.name}"),
                )
            }
        }
        Text(
            policyHint(draft.policy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SectionHeader(title = "Decay", meta = LaneEditorLogic.formatDecay(draft.decayMinutes))
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).testTag("lane_editor_decay"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val presets = (LaneEditorLogic.decayPresets + draft.decayMinutes).distinct().sorted()
            presets.forEach { minutes ->
                FilterChip(
                    selected = draft.decayMinutes == minutes,
                    onClick = { viewModel.update { it.copy(decayMinutes = minutes) } },
                    label = { Text(LaneEditorLogic.formatDecay(minutes)) },
                )
            }
        }

        SectionHeader(title = "Sound")
        OutlinedTextField(
            value = draft.sound,
            onValueChange = { v -> viewModel.update { it.copy(sound = v.take(200)) } },
            label = { Text("Sound preset or URI") },
            placeholder = { Text("Blank = policy default") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("lane_editor_sound"),
        )

        SectionHeader(title = "Breakthroughs", meta = draft.breakthroughs.size.toString())
        OutlinedTextField(
            value = state.breakthroughsText,
            onValueChange = { v -> viewModel.setBreakthroughsText(v.take(1000)) },
            label = { Text("People or keywords, comma separated") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("lane_editor_breakthroughs"),
        )

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("lane_editor_error"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = viewModel::save, enabled = !state.saving) { Text("Save lane") }
            if (!state.isNew && !draft.isDefault) {
                TextButton(onClick = viewModel::delete, modifier = Modifier.testTag("lane_editor_delete")) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun policyHint(policy: LanePolicy): String = when (policy) {
    LanePolicy.Instant -> "Delivered right away with sound."
    LanePolicy.Batched -> "Held and delivered together at batch times."
    LanePolicy.Quiet -> "Delivered silently; only visible in Doorman."
    LanePolicy.Digest -> "Rolled up into a periodic digest."
    LanePolicy.Blocked -> "Never shown; kept in history only."
}
