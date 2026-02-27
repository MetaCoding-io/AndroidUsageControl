package com.usagecontrol.feature.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.model.Rule
import com.usagecontrol.core.ui.component.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleListScreen(
    onNavigateBack: () -> Unit,
    onCreateRule: (String?) -> Unit,
    onEditRule: (Long) -> Unit,
    viewModel: RuleListViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val pillarTitle = viewModel.pillar?.let { "${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} Rules" } ?: "All Rules"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pillarTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateRule(viewModel.pillar?.name) },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create rule")
            }
        },
    ) { padding ->
        if (rules.isEmpty()) {
            EmptyState(
                message = "No rules yet. Tap + to create one.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(rules, key = { it.id }) { rule ->
                    RuleListItem(
                        rule = rule,
                        onClick = { onEditRule(rule.id) },
                        onToggle = { viewModel.toggleRule(rule.id, it) },
                        onDelete = { viewModel.deleteRule(rule) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleListItem(
    rule: Rule,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (rule.isCompound) {
                        Spacer(Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Compound", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = rule.conditionSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = rule.actionSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (rule.priority > 0) {
                    Text(
                        text = "Priority: ${rule.priority}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle,
            )

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Rule") },
            text = { Text("Delete \"${rule.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
