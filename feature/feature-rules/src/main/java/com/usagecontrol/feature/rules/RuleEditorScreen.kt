package com.usagecontrol.feature.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: RuleEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Rule" else "Create Rule") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = state.name.isNotBlank() && state.actions.isNotEmpty(),
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Rule name
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Rule name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Pillar selector
            item {
                PillarSelector(
                    selected = state.pillar,
                    onSelect = { viewModel.updatePillar(it) },
                    enabled = !state.isEditing,
                )
            }

            // Priority
            item {
                PrioritySelector(
                    priority = state.priority,
                    onUpdate = { viewModel.updatePriority(it) },
                )
            }

            // Condition groups header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Conditions", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { viewModel.addConditionGroup() }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add OR group")
                    }
                }
            }

            // Condition groups
            itemsIndexed(state.conditionGroups) { groupIndex, group ->
                ConditionGroupCard(
                    groupIndex = groupIndex,
                    group = group,
                    showOrDivider = groupIndex > 0,
                    canRemove = state.conditionGroups.size > 1,
                    onAddCondition = { condition -> viewModel.addCondition(groupIndex, condition) },
                    onRemoveCondition = { condIndex -> viewModel.removeCondition(groupIndex, condIndex) },
                    onRemoveGroup = { viewModel.removeConditionGroup(groupIndex) },
                )
            }

            // Actions header
            item {
                Text("Actions", style = MaterialTheme.typography.titleMedium)
            }

            // Actions list
            item {
                ActionsSection(
                    actions = state.actions,
                    pillar = state.pillar,
                    onAddAction = { viewModel.addAction(it) },
                    onRemoveAction = { viewModel.removeAction(it) },
                )
            }
        }
    }
}

@Composable
private fun PillarSelector(
    selected: RulePillar,
    onSelect: (RulePillar) -> Unit,
    enabled: Boolean,
) {
    Column {
        Text("Pillar", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RulePillar.entries.forEach { pillar ->
                FilterChip(
                    selected = selected == pillar,
                    onClick = { if (enabled) onSelect(pillar) },
                    label = { Text(pillar.displayName()) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun PrioritySelector(
    priority: Int,
    onUpdate: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Priority", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(16.dp))
        Slider(
            value = priority.toFloat(),
            onValueChange = { onUpdate(it.toInt()) },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text("$priority", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ConditionGroupCard(
    groupIndex: Int,
    group: ConditionGroup,
    showOrDivider: Boolean,
    canRemove: Boolean,
    onAddCondition: (RuleCondition) -> Unit,
    onRemoveCondition: (Int) -> Unit,
    onRemoveGroup: () -> Unit,
) {
    var showAddCondition by remember { mutableStateOf(false) }

    Column {
        if (showOrDivider) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Group ${groupIndex + 1} (AND)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        IconButton(onClick = { showAddCondition = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Add condition", modifier = Modifier.size(18.dp))
                        }
                        if (canRemove) {
                            IconButton(onClick = onRemoveGroup, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove group", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (group.conditions.isEmpty()) {
                    Text(
                        "No conditions — tap + to add",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    group.conditions.forEachIndexed { index, condition ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                condition.type.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    condition.type.displayName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    condition.displayParams(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = { onRemoveCondition(index) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (index < group.conditions.lastIndex) {
                            Text(
                                "AND",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 26.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCondition) {
        AddConditionDialog(
            onDismiss = { showAddCondition = false },
            onAdd = {
                onAddCondition(it)
                showAddCondition = false
            },
        )
    }
}

@Composable
private fun ActionsSection(
    actions: List<RuleAction>,
    pillar: RulePillar,
    onAddAction: (RuleAction) -> Unit,
    onRemoveAction: (Int) -> Unit,
) {
    var showAddAction by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (actions.isEmpty()) {
                Text(
                    "No actions configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                actions.forEachIndexed { index, action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            action.type.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                action.type.displayName(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (action.params.isNotEmpty()) {
                                Text(
                                    action.displayParams(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRemoveAction(index) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showAddAction = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add action")
            }
        }
    }

    if (showAddAction) {
        AddActionDialog(
            pillar = pillar,
            onDismiss = { showAddAction = false },
            onAdd = {
                onAddAction(it)
                showAddAction = false
            },
        )
    }
}
