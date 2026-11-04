package com.usagecontrol.feature.notifications.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usagecontrol.core.ui.component.EmptyState
import com.usagecontrol.core.ui.component.LoadingScreen
import com.usagecontrol.core.ui.component.SectionHeader
import com.usagecontrol.core.ui.component.Sparkline
import com.usagecontrol.feature.notifications.AppIcon

/** Apps tab: every app that notified in the last week, busiest first. */
@Composable
fun AppsScreen(
    onOpenApp: (packageName: String) -> Unit,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppsContent(state = state, onOpenApp = onOpenApp)
}

@Composable
private fun AppsContent(state: AppsUiState, onOpenApp: (String) -> Unit) {
    if (state.loading) {
        LoadingScreen(modifier = Modifier.testTag("apps_loading"))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().testTag("apps_list")) {
        item(key = "header") {
            SectionHeader(title = "Alerts per day", meta = "last ${state.windowDays}d")
        }
        if (state.rows.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    message = if (state.error) {
                        "Couldn't load app stats."
                    } else {
                        "No notifications in the last ${state.windowDays} days."
                    },
                    modifier = Modifier.padding(vertical = 32.dp).testTag("apps_empty"),
                )
            }
        }
        items(state.rows, key = { it.packageName }) { row ->
            AppRow(row = row, onClick = { onOpenApp(row.packageName) })
        }
    }
}

@Composable
private fun AppRow(row: AppRowUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("apps_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = row.packageName, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "you dismiss ${row.dismissPercent}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("apps_dismiss_hint"),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Sparkline(values = row.series.map { it.toFloat() })
            Text(
                text = AppsLogic.formatPerDay(row.alertsPerDay),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
